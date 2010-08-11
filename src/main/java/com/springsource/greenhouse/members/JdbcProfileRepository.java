package com.springsource.greenhouse.members;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.inject.Inject;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class JdbcProfileRepository implements ProfileRepository {

	private final JdbcTemplate jdbcTemplate;

	@Inject
	public JdbcProfileRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}
	
	public Profile findByKey(String profileKey) {
		Long accountId = getAccountId(profileKey);
		return accountId != null ? findByAccountId(accountId) : findByUsername(profileKey);
	}
	
	public Profile findByAccountId(Long accountId) {
		return jdbcTemplate.queryForObject(SELECT_PROFILE + " where id = ?", profileMapper, accountId);
	}

	public List<ConnectedProfile> findConnectedProfiles(Long accountId) {
		return jdbcTemplate.query("select provider, accountId from ConnectedAccount where member = ? order by provider", new RowMapper<ConnectedProfile>() {
			public ConnectedProfile mapRow(ResultSet rs, int row) throws SQLException {
				String provider = rs.getString("provider");
				String accountId = rs.getString("accountId");
				return new ConnectedProfile(provider, getProfileUrl(accountId, provider));
			}
		}, accountId);
	}
	
	// internal helpers
	
	private Profile findByUsername(String username) {
		return jdbcTemplate.queryForObject(SELECT_PROFILE + " where username = ?", profileMapper, username);
	}
	
	private RowMapper<Profile> profileMapper = new RowMapper<Profile>() {
		public Profile mapRow(ResultSet rs, int row) throws SQLException {
			return new Profile(rs.getLong("id"), rs.getString("displayName"), rs.getString("pictureUrl"));
		}
	};

	private Long getAccountId(String id) {
		try {
			return Long.parseLong(id);
		} catch (NumberFormatException e) {
			return null;
		}		
	}
	
	private String getProfileUrl(String accountId, String provider) {
		if ("Twitter".equals(provider)) {
			return "http://www.twitter.com/" + accountId;
		} else if ("Facebook".equals(provider)) {
			return "http://www.facebook.com/profile.php?id=" + accountId;
		} else {
			return null;
		}
	}

	private static final String SELECT_PROFILE = "select id, (firstName || ' ' || lastName) as displayName, pictureUrl from Member";
	
}