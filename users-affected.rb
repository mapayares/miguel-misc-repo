#!/usr/bin/env ruby

require 'json'
require 'fileutils'
require 'pry'

POSTGRES_FILE = "/etc/tealium/postgres_users.json"
MONGO_FILE = "/etc/tealium/mongo_users.json"

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
end

def getUsers(file, postgres_users)
  users = Array.new

  json_field = postgres_users ? 'last_modified_by' : 'email'
  puts "Getting users for postgres/mongo users\n"
  file.each do |doc|
    user = doc.fetch(json_field)
    users.push(user)
  end

  if users.empty?
    puts "FRACASAR: Failed to get the users to cross reference\n"
    exit 1
  end
  return users.uniq
end

def getAffectedUsers(postgres_users, mongo_users)
  puts "Searching for users that will be affected by the new permissions \n"
  affected_users = Array.new

  postgres_users.each do |user|
    if mongo_users.include?(user)
      affected_users.push(user)
    end
  end

  return affected_users
end

def getStats(users)
  puts "Getting stats for the users that will be affected by new publish permissions \n"

  if users.empty?
    puts "There are no users that will be affected by the new permissions \n"
    return
  end

  users.each do |user|
    puts "THIS IS THE USER NAME: #{user} THAT WILL BE AFFECTED \n"
  end

  count = users.size
  puts "THIS IS THE NUMBER OF PEOPLE THAT WILL BE AFFECTED BY THE NEW PERMISSIONS #{count} \n"
end

if __FILE__ == $PROGRAM_NAME
  postgres_file = getConfigFile(POSTGRES_FILE)
  mongo_file = getConfigFile(MONGO_FILE)

  postgres_users = getUsers(postgres_file, true)
  mongo_users = getUsers(mongo_file, false)

  affected_users = getAffectedUsers(postgres_users, mongo_users)
  getStats(affected_users)
  exit 0
end
