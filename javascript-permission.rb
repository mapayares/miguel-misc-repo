#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'
require 'mongo'

JAVASCRIPT_EXTENSION_PERM_CONST = "EXTENSIONS_JAVASCRIPT"
JAVASCRIPT_PROFILE_PERM_CONST = "JS_DRAFT_PROMOTION"
EMAIL_CONST = "email"
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

def createNewPermission(users_coll, permission_coll)
  puts "Querying get all users that need this new profile Javascript permission\n"
  documents = permission_coll.find(:permissions => {"$in" => [JAVASCRIPT_EXTENSION_PERM_CONST]}).sort([EMAIL_CONST, 1])

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
      end #closes second loop
    end #closes first loop
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
  javascript_perm = "tealium:accounts:" + account + ":profiles:" + profile + ":js_draft_promotion"
  begin
    result = users_coll.update_one( { :email => email }, { "$addToSet" => { :permissions => javascript_perm}})
    raise Exception, "Fail to update user: #{email} for collection: #{users_coll}\n" unless result.n == 1
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
    result = permission_coll.update_one( { :email => email, :account => account}, { "$addToSet" => {"profiles.#{profile}.permissions" =>
      JAVASCRIPT_PROFILE_PERM_CONST} } )
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

  config_location = getCommandArguments
  config = getConfigFile(config_location)

  mongo_host, mongo_db, users, permission = getMongoValues(config)

  puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
  mongo_client, users_coll, permission_coll = getMongoCoreDB(mongo_host, mongo_db, users, permission)

  puts "Creating new Profile Level Permission\n"
  createNewPermission(users_coll, permission_coll)

  puts "Creating new profile permission Script Finish \n"
  mongo_client.close
  puts "Here is all the users that super user array found \n"
  puts $tealim_super_user
  exit 0
end
