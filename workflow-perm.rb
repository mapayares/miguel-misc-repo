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

def getUsersDbRecord(users_perm_cache_array, users_coll, perm_cache_coll)

  users_perm_cache_array.each do | user_record |

    email = user_record.fetch(EMAIL_CONST)
    account_name = user_record.fetch('account')

    #next if super_users.contains?(email)

    profiles = user_record.fetch('profiles')
    workflow_permission_array = Array.new

    profiles.each do | profile |
      profile_name = profile.fetch('profile')

      if ('*'.eql?(profile_name))
        workflow_permission_array.push('accounts:' + account_name + ':profiles:*:publish_workflow_approval')
        break
      else
        workflow_permission_array.push('accounts:' + account_name + ':profiles:' + profile_name + ':publish_workflow_approval')
      end
    end

    insertUserNewWorkFlowPermission(email, workflow_permission_array, users_coll)

  end


end

def updateUserNewWorkFlowPermission(email, workflow_permission_array, users_coll)
  puts "Updating user: #{email} document in MongoDB \n"
  begin
      users_coll.find_one_and_update( { :email => email}, {"$addToSet" => { :permissions => { "$each" => workflow_permission_array}}}, :upsert => false)
    rescue => e
      puts "FRACASAR: There was an error trying to update user: #{email} for collection: #{users_coll} Error: #{e}\n"
      exit 1
  end
end

def addPublishWorkflowPerm(mongo_users_coll, mongo_permission_cache, mongo_client, accounts)

  users_permission_cache_array = getUsersPermissionCache(accounts, mongo_permission_cache)
  getUsersDbRecord(users_perm_cache_array, mongo_users_coll, mongo_permission_cache)

  mongo_client.close

end

#this function will retrieves all the mongo configurations
def getMongoValues(config)
  puts "Getting Mongo configuration values\n"
  collections = config.fetch("mongo_collections")
  mongo_host = config.fetch("mongo_host")
  mongo_db = config.fetch("mongo_db")
  accounts = config.fetch("accounts")

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

  return mongo_host, mongo_db, users_coll, permission_cache_coll, accounts
end

if __FILE__ == $PROGRAM_NAME
  config_location = getCommandArguments
  config = getConfigFile(config_location)
  mongo_host, mongo_db, users_coll, permission_cache_coll, accounts = getMongoValues(config)

  puts "Connecting to Mongo #{mongo_db} DB from host #{mongo_host}\n"
  mongo_client, mongo_users_collection, mongo_permission_cache = getMongoCoreDBCollections(mongo_host, mongo_db,
    users_coll, permission_cache_coll)

  puts "Adding new WorkFlow Permission to Users who have Profile Workflow turn on\n"
  addPublishWorkflowPerm(mongo_users_collection, mongo_permission_cache, mongo_client, accounts)

  puts "DONE WITH SCRIPT!!!!!!!\n"
  exit 0
end
