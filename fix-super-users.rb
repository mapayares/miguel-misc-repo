require 'json'
require 'fileutils'
require 'pry'
require 'mongo'

EMAIL_CONST = "email"
$tealim_super_user_create = ["tealium:accounts:*:create"]
$tealim_super_user_delete = ["tealium:accounts:*:delete"]
$star_permissions_no_delete_create = ["tealium:accounts:*:create", "tealium:accounts:*:profiles:*:create",
  "tealium:accounts:*:profiles:*:edit", "tealium:accounts:*:profiles:*:manage_users", "tealium:accounts:*:profiles:*:copy",
  "tealium:accounts:*:profiles:*:read", "tealium:accounts:*:delete"]

$tealium_super_users = ["tealium:accounts:*:profiles:*:publish_targets:qa:publish",
		"tealium:accounts:*:create", "tealium:accounts:*:profiles:*:delete", 	"tealium:accounts:*:profiles:*:templates:*:lock",
		"tealium:accounts:*:profiles:*:manage_users", "tealium:accounts:*:profiles:*:templates:*:edit",
		"tealium:accounts:*:profiles:*:templates:*:create", "tealium:accounts:*:read", "tealium:accounts:*:profiles:*:templates:*",
		"tealium:accounts:*:profiles:*:create", "tealium:accounts:*:profiles:*:edit", "tealium:accounts:*:edit",
		"tealium:accounts:*:profiles:*:copy", "tealium:accounts:*:profiles:*:templates:*:read",
		"tealium:accounts:*:profiles:*:publish_targets:prod:publish", "tealium:accounts:*:profiles:*:templates:*:delete",
		"tealium:accounts:*:profiles:*:publish_targets:dev:publish", "tealium:accounts:*:profiles:*:read",
		"tealium:accounts:*:manageaudit", "tealium:accounts:*:extensions:javascript:edit",
		"tealium:accounts:*:profiles:*:secure_labels:edit", "tealium:accounts:*:delete",
		"tealium:accounts:*:profiles:*:js_promotion:dev", "tealium:accounts:*:profiles:*:js_promotion:qa",
		"tealium:accounts:*:profiles:*:js_promotion:prod"]

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

def getMongoCoreDB(mongo_host, mongo_db, users)
  puts "Attemptig to connect to MongoDB\n"
  client = Mongo::Client.new([mongo_host], :database => mongo_db)

  puts "Getting the users collection\n"
  users_coll = client[users]

  unless users_coll
    raise Exception, "Failed to get the user collection"
  end
  return client, users_coll
end

#this function will retrieves all the mongo configurations
def getMongoValues(config)
  puts "Getting Mongo configuration values\n"
  mongo_coll = config.fetch("mongo_collections")
  mongo_host = config.fetch("mongo_host")
  mongo_db = config.fetch("mongo_db")

  user_collection = String.new
  mongo_coll.each do | coll |
    if coll.eql?('users')
      user_collection = coll
    end
  end

    if user_collection.empty?
      raise ArgumentError, "Could not find User and Permission collection"
    end
  return mongo_host, mongo_db, user_collection
end

def getAllSuperUsers(mongo_client, users_coll, wanted_users)
  puts "Getting all of Tealium super users\n"
  users = Array.new
  super_users = users_coll.find({ :permissions => { "$all" => wanted_users}}, {:email => 1})
  super_users.each do |user|
    email = user.fetch(EMAIL_CONST)
    users.push(email)
  end
  return users
end

def insertFixedPermissions(mongo_client, users_coll, users, permission_type)
  users.each do |user|
    puts "Fixing Tealium user : #{user}\n"
    users_permissions = $tealium_super_users
    case permission_type
      when "delete"
        users_permissions.delete("tealium:accounts:*:create")
      when "create"
        users_permissions.delete("tealium:accounts:*:delete")
    end
    users_coll.update_one( { EMAIL_CONST => user}, {"$set" => { :permissions => users_permissions }})
  end
end

if __FILE__ == $PROGRAM_NAME
  begin
    config_location = getCommandArguments
    config = getConfigFile(config_location)

    mongo_host, mongo_db, users = getMongoValues(config)

    puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
    mongo_client, users_coll = getMongoCoreDB(mongo_host, mongo_db, users)

    puts "Fixing Tealium super users permissions\n"
    super_users = getAllSuperUsers(mongo_client, users_coll, $star_permissions_no_delete_create)
    delete_account_users = getAllSuperUsers(mongo_client, users_coll, $tealim_super_user_delete)
    create_account_users = getAllSuperUsers(mongo_client, users_coll, $tealim_super_user_create)

    only_delete_users_perms = delete_account_users - super_users
    only_create_users_perms = create_account_users - super_users

    delete_create_users_perms = only_create_users_perms + only_delete_users_perms
    all_existing_users = super_users - delete_create_users_perms
    binding.pry

    insertFixedPermissions(mongo_client, users_coll, only_create_users_perms, "create")
    insertFixedPermissions(mongo_client, users_coll, only_delete_users_perms, "delete")
    insertFixedPermissions(mongo_client, users_coll, all_existing_users, "all")
    puts "Creating new profile permission Script Finish \n"
  ensure
    puts "Closing MongoDB connection\n"
    mongo_client.close
  end

  puts "Here is all the super users that the script found \n"
  exit 0
end
