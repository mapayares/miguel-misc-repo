package com.messages.services;

import static org.springframework.ldap.query.LdapQueryBuilder.query;

import java.util.List;

import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.filter.AndFilter;
import org.springframework.ldap.filter.EqualsFilter;
import org.springframework.ldap.query.LdapQuery;
import org.springframework.ldap.query.LdapQueryBuilder;
import org.springframework.stereotype.Service;

@Service
public class LdapService {

	private final Logger logger = LoggerFactory.getLogger(LdapService.class);

	@Autowired
	private LdapTemplate ldapTemplateService;

	private static final String USER_OBJECT_CLASS = "person";
	private static final String USER_ATTRIBUTE = "uid";
	private static final String OBJECT_CLASS = "objectclass";

	/**
	 * Retrieves all the persons in the ldap server
	 *
	 * @return list of person names
	 */
	public List<String> getAllPersonNames() {
		return ldapTemplateService.search(query().where(OBJECT_CLASS).is(USER_OBJECT_CLASS), new AttributesMapper<String>() {
			public String mapFromAttributes(Attributes attrs) throws NamingException {
				return (String) attrs.get(USER_ATTRIBUTE).get();
			}
		});
	}

	public boolean login(String userName, String password) {
		AndFilter andFilter = new AndFilter();
		andFilter.and(new EqualsFilter(USER_ATTRIBUTE, userName)).and(new EqualsFilter("password", password));
		EqualsFilter filter = new EqualsFilter(USER_ATTRIBUTE, userName);

		LdapQuery query = LdapQueryBuilder.query().filter(filter);

		List<String> users = ldapTemplateService.search(query, new AttributesMapper<String>() {
			public String mapFromAttributes(Attributes attrs) throws NamingException {
				return (String) attrs.get(USER_ATTRIBUTE).get();
			}
		});

		if (users.size() == 1) {
			return true;
		}
		return false;
	}

}
