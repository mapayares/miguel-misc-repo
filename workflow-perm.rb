#!/usr/bin/env ruby
require 'json'
require 'fileutils'
require 'pry'
require 'mongo'

WORKFLOW_PERMISION_APPROVAL = "PUBLISH_WORKFLOW_APPROVAL"
EMAIL_CONST = "email"
PUBLISH_PROD_CONST = "PUBLISH_PROD"

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

def getMongoCoreDBCollections(mongo_host, mongo_db, users_coll, permission_cache_coll)
  puts "Attemptig to connect to MongoDB\n"
  client = Mongo::Client.new([mongo_host], :database => mongo_db)

  puts "Getting the permission cache collection \n"
  mongo_permission_cache = client[permission_cache_coll]

  puts "Getting the users collection for mongo \n"
  mongo_users_coll = client[users_coll]

  raise Exception, "Failed to get the user collection and permission cache collection" unless mongo_users_coll and mongo_permission_cache
  return client, mongo_users_coll, mongo_permission_cache
end

def getUsersPermissionCache(accounts, permission_cache_coll)
  users_perm_array = Array.new
  puts "Getting users permission cache record whose account have workflow turn on \n"
  accounts.each do | acc |
    user_permission_doc = permission_cache_coll.find(:account => acc)
    users_perm_array.push(user_permission_doc)
  end
  return users_perm_array
end

def updateUsersWorkFlowPermission(users_perm_cache_array, users_coll, perm_cache_coll, super_users)
  users_perm_cache_array.each do | user_record |

    puts "THESE ARE THE AMOUNT OF RECORDS WE FOUND: #{user_record.count} \n\n"

    puts "Iterating through all MongoDB records that have Workflow On \n"
    user_record.each do | record |
      email = record.fetch(EMAIL_CONST)
      account_name = record.fetch('account')
      next if super_users.include?(email)

      next unless record.has_key?('profiles')
      profiles = record.fetch('profiles')

      user_permission_array, permission_cache_array, profile_name = isPublishProdEnable(profiles, email, account_name)

      next if profile_name.empty?
      updateUserDocument(email, user_permission_array, users_coll)
      updateUserPermissionCache(email, account_name, profile_name, permission_cache_array, perm_cache_coll)
    end
  end
end

def isPublishProdEnable(profiles, email, account_name)
  user_permission_array = Array.new
  profile_permissions = Array.new
  profile_name = String.new
  profiles.each do | profile |
    profile_data = profile.pop
    next unless profile_data.has_key?('profile')
    profile_name = profile_data.fetch('profile')

    profile_permissions = profile_data.fetch('permissions')
    next unless (profile_permissions.include?(PUBLISH_PROD_CONST))

    profile_permissions.push(WORKFLOW_PERMISION_APPROVAL) unless profile_permissions.include?(WORKFLOW_PERMISION_APPROVAL)
    if ('*'.eql?(profile_name))
      user_permission_array.push('tealium:accounts:' + account_name + ':profiles:*:publish_workflow_approval')
      break
    else
      user_permission_array.push('tealium:accounts:' + account_name + ':profiles:' + profile_name + ':publish_workflow_approval')
    end
  end
  return user_permission_array, profile_permissions, profile_name
end

def updateUserDocument(email, user_permission_array, users_coll)
  puts "Updating user: #{email} permissions in MongoDB \n"
  begin
      users_coll.find_one_and_update( { :email => email}, {"$addToSet" => { :permissions => { "$each" => user_permission_array}}}, :upsert => false)
    rescue => e
      puts "FRACASAR: There was an error trying to update user: #{email} for collection: #{users_coll} Error: #{e}\n"
      exit 1
  end
end

def updateUserPermissionCache(email, account, profile_name, permission_cache_array, permission_coll)
  puts "Updating user: #{email} permission cache with WorkFlow Permissions \n"
  begin
    permission_coll.find_one_and_update( { :email => email, :account => account}, { "$set" => { "profiles.#{profile_name}.permissions" => permission_cache_array}})
  rescue => e
    puts "FRACASAR: There was an error trying to update user: #{email} for collection: #{permission_coll} Error: #{e}\n"
    exit 1
  end
end

def addPublishWorkflowPerm(users_coll, permission_cache_coll, mongo_client, accounts, super_users)
  users_perm_cache_array = getUsersPermissionCache(accounts, permission_cache_coll)
  updateUsersWorkFlowPermission(users_perm_cache_array, users_coll, permission_cache_coll, super_users)

  mongo_client.close
end

#this function will retrieves all the mongo configurations
def getMongoValues(config)
  puts "Getting Mongo configuration values\n"
  collections = config.fetch("mongo_collections")
  mongo_host = config.fetch("mongo_host")
  mongo_db = config.fetch("mongo_db")
  accounts = config.fetch("accounts")
  super_users = config.fetch("super_users")

  raise ArgumentError, "Could not find Permission collection" if collections.empty?
  raise ArgumentError, "Could not find Permission collection" if accounts.empty?

  users_coll = nil
  permission_cache_coll = nil
  collections.each do | coll |
    if coll.eql?('users')
      users_coll = coll
    elsif coll.eql?('permission_cache')
      permission_cache_coll = coll
    end
  end

  return mongo_host, mongo_db, users_coll, permission_cache_coll, accounts, super_users
end

if __FILE__ == $PROGRAM_NAME
  config_location = getCommandArguments
  config = getConfigFile(config_location)
  mongo_host, mongo_db, users_coll, permission_cache_coll, accounts, super_users = getMongoValues(config)

  puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
  mongo_client, mongo_users_collection, mongo_permission_cache = getMongoCoreDBCollections(mongo_host, mongo_db,
    users_coll, permission_cache_coll)

  puts "Adding new WorkFlow Permission to Users who have Profile Workflow turn on\n"
  addPublishWorkflowPerm(mongo_users_collection, mongo_permission_cache, mongo_client, accounts, super_users)

  puts "DONE WITH SCRIPT!!!!!!!\n"
  exit 0
end
