package com.messages;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.jdbc.JdbcPooledConnectionSource;
import com.j256.ormlite.stmt.PreparedQuery;
import com.messages.errors.SQLDBError;
import com.messages.models.UserMessages;

@Singleton
@Component
public class MessageDao {

	private final Logger logger = LoggerFactory.getLogger(MessageDao.class);
	private JdbcPooledConnectionSource connPool;

	public MessageDao() {

		connPool = null;

		try {
			Class.forName("com.mysql.jdbc.Driver");
			connPool = new JdbcPooledConnectionSource(StringConstants.dbURL, StringConstants.dbUserName, StringConstants.dbPwd);
		} catch (ClassNotFoundException ee) {
			logger.error("fail to reigster class" + ee);
		} catch (SQLException e) {
			logger.error("Failed to connect to MySQL database host: " + StringConstants.dbURL, e);
			System.exit(0);
		}
		connPool.setMaxConnectionsFree(5);
		connPool.setMaxConnectionAgeMillis(60 * 60 * 1000);
		connPool.setCheckConnectionsEveryMillis(30 * 1000);
	}

	public List<UserMessages> getMessages(String userName) {
		List<UserMessages> messages = new ArrayList<>();
		Dao<UserMessages, Integer> dao = null;
		try {
			dao = DaoManager.createDao(connPool, UserMessages.class);
			messages = dao.queryBuilder().orderBy("timestamp", true).limit(100L).where().eq(StringConstants.USERNAME, userName).query();
		} catch (SQLException e) {
			throw new SQLDBError("Fail to get user: " + userName + " 100 messages", e);
		}
		return messages;
	}

	public UserMessages findUser(String userName) {
		Dao<UserMessages, Integer> dao = null;
		UserMessages user = null;
		try {
			dao = DaoManager.createDao(connPool, UserMessages.class);
			PreparedQuery query = dao.queryBuilder().where().eq(StringConstants.USERNAME, userName).prepare();
			 user = dao.queryForFirst(query);
			return user;
		} catch (SQLException e) {
			throw new SQLDBError("Fail to get user: {} " + userName + " from DB", e);
		}
	}

}
