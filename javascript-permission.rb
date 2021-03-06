#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'
require 'mongo'

JAVASCRIPT_EXTENSION_PERM_CONST = "EXTENSIONS_JAVASCRIPT"
JAVASCRIPT_PROFILE_PERM_CONST = ["DEV_JS_PROMOTION", "QA_JS_PROMOTION", "PROD_JS_PROMOTION"]
EMAIL_CONST = "email"
TMP_JS_PERM_COLL = 'tmp_js_perm_coll'
$tealim_super_user = Array.new
$star_permissions = ["tealium:accounts:*:read", "tealium:accounts:*:create", "tealium:accounts:*:profiles:*:create",
  "tealium:accounts:*:profiles:*:edit", "tealium:accounts:*:profiles:*:manage_users", "tealium:accounts:*:edit",
  "tealium:accounts:*:profiles:*:copy", "tealium:accounts:*:profiles:*:read", "tealium:accounts:*:create"]


def getConfigFile(config)
  puts "reading config file to get proper configuration"

  file = nil
  config_values = nil
  begin
    file = File.read(config)
    config_values = JSON.parse(file)
  rescue => e
    puts "FRACASAR: There was an error trying to read the config file : #{e}"
    exit 1
  end

  puts "Successfully read the config file"
  return config_values
end

def getCommandArguments
  puts "getting command line argument for location of config"
  config_location = nil
  if ARGV.length > 0
    config_location = ARGV[0]
  end

  config_location = "/etc/tealium/mongo_config.json" unless config_location

  puts "Configuration file location is at : #{config_location}"
  return config_location
end

def getMongoCoreDB(mongo_host, mongo_db, users, permission)
  puts "Attemptig to connect to MongoDB\n"
  client = Mongo::Client.new([mongo_host], :database => mongo_db)

  puts "Getting the users collection\n"
  users_coll = client[users]

  puts "Getting the permission cache collection \n"
  permission_coll = client[permission]

  unless users_coll and permission_coll
    raise Exception, "Failed to get the user collection and permission cache collection"
  end
  return client, users_coll, permission_coll
end

def createNewPermissionTTLColl(mongo_client, permission_coll)
  puts "Checking if we need to create temporary JS PERM collection that expires all documents in 5 days\n"
  ttl_needed = false
  tmp_js_perm_coll = mongo_client[TMP_JS_PERM_COLL]
  count = tmp_js_perm_coll.count
  if (count == 0)
    puts "Creating TTL index to expire all documents in 5 days\n"
    tmp_js_perm_coll.indexes.create_one( {:expired_at => 1}, :expire_after_seconds => 2592000)
    ttl_needed = true
  end

  if (ttl_needed)
    insertUsersPermNeeded(permission_coll, tmp_js_perm_coll)
  end
  return tmp_js_perm_coll
end

def insertUsersPermNeeded(permission_coll, tmp_js_perm_coll)
  puts "Inserting all users that need this new js permission \n"
  documents = permission_coll.find(:permissions => {"$in" => [JAVASCRIPT_EXTENSION_PERM_CONST]}).distinct(:email)

  puts "This is the number of users that need to be update with the new permissions #{documents.count}\n"
  users_perm = Array.new
  documents.each do |email|
    perm = { :email => email }
    users_perm.push(perm)
  end

  result = tmp_js_perm_coll.insert_many(users_perm)
  if (result.inserted_count != documents.count)
    raise Exception, "Failed to insert all the users in the ttl temporary collection\n\n"
  end

end

def createNewPermission(users_coll, permission_coll, tmp_js_perm_coll)
  puts "Querying get all users that need this new profile Javascript permission\n"

  js_users = Array.new
  tmp_documents = tmp_js_perm_coll.find
  puts "Here is the remaining documents for the script to finish #{tmp_documents.count}\n\n"
  raise Exception, "Failed to get MongoDB records from temporary JS Collection\n" if tmp_documents.count == 0
  js_users = tmp_documents

  js_users.each do |user|
    email_user = user[:email]
    documents = permission_coll.find({ :email => email_user, :permissions => {"$in" => [JAVASCRIPT_EXTENSION_PERM_CONST] }})
    documents.each do |doc|
        email = doc.fetch(EMAIL_CONST)
        profiles_obj = doc.fetch("profiles")
        account = doc.fetch("account")
        profiles_obj.each do |profiles|
          profile = profiles.fetch(0)
          if "*".eql?(profile)
            updateStarUsers(users_coll, permission_coll, email, account, profile)
          else
            updateUsersPermissions(users_coll, email, account, profile)
            updatePermissionCache(permission_coll, email, account, profile)
          end
        end #closes third loop
      end #closes second loop
      tmp_js_perm_coll.delete_one(:email => email_user)
  end # closes the first loop
end

def updateStarUsers(users_coll, permission_coll, email, account, profile)
  puts "Updating star user: #{email} for Account: #{account}\n"
  puts "Determine if user: #{email} is a Super User or a simply * account user\n"
  if email.include?("@tealium")
    updateTealiumUser(users_coll, permission_coll, email, account, profile)
  else
    updateUsersPermissions(users_coll, email, account, profile)
    updatePermissionCache(permission_coll, email, account, profile)
  end
end

#updates Tealium user permissions and permission cache object
def updateTealiumUser(users_coll, permission_coll, email, account, profile)
  puts "User: #{email} is a tealium need to see if he is a Super User\n"

  if $tealim_super_user.include?(email)
    puts "User: #{email} is a super user whose user permissions have been updating\n"
    puts "Just updating their permission cache object for Account: #{account}, Profile: #{profile}\n"
    updatePermissionCache(permission_coll, email, account, profile)
    return
  end

  puts "Needs to find out if Tealium User: #{email} is a super user \n"
  result = users_coll.find( :email => email, :permissions => { "$all" => $star_permissions } )

  if result.count == 1
    puts "Tealium User: #{email} is a super user in our system adding */* permissions \n"
    updateUsersPermissions(users_coll, email, "*", "*")
    updatePermissionCache(permission_coll, email, account, profile)
    $tealim_super_user.push(email)
    return
  end
  updateUsersPermissions(users_coll, email, account, profile)
  updatePermissionCache(permission_coll, email, account, profile)
end

#this function will update the users permissions to reflect the new javascript permission
def updateUsersPermissions(users_coll, email, account, profile)
  puts "Updating user: #{email} permission to include the new Profile level JavaScript permission\n"
  javascript_perm = ["tealium:accounts:" + account + ":profiles:" + profile + ":js_promotion:dev",
                     "tealium:accounts:" + account + ":profiles:" + profile + ":js_promotion:qa",
                     "tealium:accounts:" + account + ":profiles:" + profile + ":js_promotion:prod"]
  begin
    result = users_coll.find_one_and_update( { :email => email }, { "$addToSet" => { :permissions => { "$each" => javascript_perm }}}, :upsert => false)
    if (result == nil)
      "FAIL to update USER: #{email} for COLLECTION: #{users_coll}\n"
    else
      raise Exception, "Fail to update user: #{email} for collection: #{users_coll}\n" unless result.has_key?(:email)
    end
  rescue => e
    puts "FRACASAR: There was an error trying to update user: #{email} for collection: #{users_coll} Error: #{e}\n"
    exit 1
  end
end

#this function will update the permission cache object to reflect the
#new javascript permission at the profile level
def updatePermissionCache(permission_coll, email, account, profile)
  puts "Updating user: #{email} permission cache object to the new Profile leve JavaScript permission\n"
  begin
    result = permission_coll.update_one( { :email => email, :account => account}, { "$addToSet" => {"profiles.#{profile}.permissions" => {
      "$each" => JAVASCRIPT_PROFILE_PERM_CONST} } } )
    raise Exception, "Fail to update user: #{email} for collection: #{permission_coll}\n" unless result.n == 1
  rescue => e
    puts "FRACASAR: There was an error trying to update user: #{email} for collection: #{permission_coll} Error: #{e}\n"
    exit 1
  end
end

#this function will retrieves all the mongo configurations
def getMongoValues(config)
  puts "Getting Mongo configuration values\n"

  mongo_coll = config.fetch("mongo_collections")
  mongo_host = config.fetch("mongo_host")
  mongo_db = config.fetch("mongo_db")

  user_collection = String.new
  permission_collection = String.new
  mongo_coll.each do | coll |
    if coll.eql?('users')
      user_collection = coll
    elsif coll.eql?('permission_cache')
      permission_collection = coll
    end
  end

    if user_collection.empty? and permission.collection.empty?
      raise ArgumentError, "Could not find User and Permission collection"
    end
  return mongo_host, mongo_db, user_collection, permission_collection
end

if __FILE__ == $PROGRAM_NAME
  begin
    config_location = getCommandArguments
    config = getConfigFile(config_location)

    mongo_host, mongo_db, users, permission = getMongoValues(config)

    puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
    mongo_client, users_coll, permission_coll = getMongoCoreDB(mongo_host, mongo_db, users, permission)

    puts "Creating temporary TTL Collection to keep track of all users that have no received the new JS permissions \n"
    tmp_js_perm_coll = createNewPermissionTTLColl(mongo_client, permission_coll)

    puts "Creating new Profile Level Permission\n"
    createNewPermission(users_coll, permission_coll, tmp_js_perm_coll)

    puts "Creating new profile permission Script Finish \n"
  ensure
    puts "Closing MongoDB connection\n"
    mongo_client.close
  end

  puts "Here is all the super users that the script found \n"
  puts $tealim_super_user
  exit 0
end
