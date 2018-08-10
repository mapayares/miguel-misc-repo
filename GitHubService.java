package com.tealium.resources.legacy.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.tealium.util.StringConstants;

@Singleton
public class GitIntegration {
	
	//private static final String apiToken = "346765c25530dcc3d24bd3f6da5559af10f7d987";
	//private static final String gitHubURL = "https://api.github.com";
	private static final Logger logger = LoggerFactory.getLogger(GitIntegration.class);	
	private final String gitHubApiUrl;
	private final AccountService accountService;
	private final HttpService httpService;
	private static final String GIT_REPOS = "/repos";
	private static final String backSlash = "/";

	@Inject
	public GitHubService(AccountService accountService, @Named("github_api_url") String gitHubUrl, HttpService httpService) {
		this.gitHubApiUrl = gitHubUrl;
		this.accountService = accountService;
		this.httpService = httpService;
	}
	
	public String uploadGitFile() throws UnsupportedOperationException, IOException {
		logger.info("Uploading file to Miguel repo");
		
		String encoded = Base64.getEncoder().encodeToString("this is a java file that i am creating".getBytes());
		
		JsonObject json = new JsonObject();
		JsonObject committer = new JsonObject();
		committer.addProperty("name", "mapayares");
		committer.addProperty("email", "mapayares@yahoo.com");
		json.addProperty("message", "new java file");
		json.addProperty("content", encoded);
		json.addProperty("branch", "test-branch");
		json.add("committer", committer);
		
		StringEntity payload = new StringEntity(json.toString());
		
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPut putCall = new HttpPut(gitHubURL + "/repos/mapayares/miguel-test-repo/contents/miguel-java.java");
		putCall.addHeader("Accept:", "application/vnd.github.v3+json");
		putCall.addHeader("Authorization:", "token " + apiToken);
		putCall.addHeader("Content-Type:", "application/vnd.github.VERSION.raw");
		putCall.setEntity(payload);
		HttpResponse response = null;
		try {
			response = client.execute(putCall);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		HttpEntity entity = response.getEntity();
		
		String content = null;
		if (entity != null) {
			content = CharStreams.toString(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
			EntityUtils.consume(entity); 
		}
		
		HttpClientUtils.closeQuietly(client);
		return content;		
	}
	
	public String updateGitFile() throws UnsupportedOperationException, IOException {
		logger.info("Uploading file to Miguel repo");
		
		String encoded = Base64.getEncoder().encodeToString("miguel is updating this file".getBytes());
		
		JsonObject json = new JsonObject();
		json.addProperty("message", "updating git file");
		json.addProperty("content", encoded);
		json.addProperty("branch", "test-branch");
		json.addProperty("sha", "d1261f6447dd4188dcfb599123070a02a6221f45");
		
		StringEntity payload = new StringEntity(json.toString());
		
		CloseableHttpClient client = HttpClients.createDefault();
		HttpPut putCall = new HttpPut(gitHubURL + "/repos/mapayares/miguel-test-repo/contents/directory/miguel.rb");
		putCall.addHeader("Accept:", "application/vnd.github.v3+json");
		putCall.addHeader("Authorization:", "token " + apiToken);
		putCall.addHeader("Content-Type:", "application/vnd.github.3.raw");
		putCall.setEntity(payload);
		HttpResponse response = null;
		try {
			response = client.execute(putCall);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		HttpEntity entity = response.getEntity();
		
		String content = null;
		if (entity != null) {
			content = CharStreams.toString(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
			EntityUtils.consume(entity); 
		}
		
		HttpClientUtils.closeQuietly(client);
		return content;		
	}
	
	public String getGitHubFile(String account, String filePath) {
		logger.debug("Getting GitHub file content for {}", filePath);
		Map<String, String> gitData = getGitHubData(filePath);
		String encodedBranchParam = encodeBranchParameter(gitData.get("git_branch"));
		HttpGet getCall = new HttpGet(gitData.get("api_url") + encodedBranchParam);

		getCall = (HttpGet) addHttpHeaders(getCall, account);
		String content = null;
		try {
			content = httpService.executeHttpRequest(getCall);	
			httpService.checkIfContentEmpty(content);
		} catch (HttpServiceException e) {
			throw new ProfileServiceException("Fail to get github file content for:" + filePath, e);
		}
		return content;	
	}
	
	public String createGitHubBranch() throws UnsupportedOperationException, IOException {
		logger.info("Creating Pull Request for Miguel Repo");
		CloseableHttpClient client = HttpClients.createDefault();
				
		HttpPost postCall = new HttpPost(gitHubURL + "/repos/mapayares/miguel-test-repo/git/refs");
		postCall.addHeader("Accept:", "application/vnd.github.v3+json");
		postCall.addHeader("Authorization:", "token " + apiToken);
		postCall.addHeader("Content-Type:", MediaType.APPLICATION_JSON);
		
		JsonObject json = new JsonObject();
		json.addProperty("ref", "refs/heads/miguel-branch");
		json.addProperty("sha", "320000d44af0947c0b9623b0e5c937c7324b76dd");
					
		StringEntity payload = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
		postCall.setEntity(payload);
		
		HttpResponse response = null;
		try {
			response = client.execute(postCall);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		HttpEntity entity = response.getEntity();
		
		String content = null;
		if (entity != null) {
			content = CharStreams.toString(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
			EntityUtils.consume(entity); 
		}
		
		HttpClientUtils.closeQuietly(client);
		return content;	
	}
	
	public String deleteGitHubPullRequest() throws UnsupportedOperationException, IOException {
		logger.info("Deleting a branch from Miguel Test Repo");
		CloseableHttpClient client = HttpClients.createDefault();
				
		HttpDelete deleteCall = new HttpDelete(gitHubURL + "/repos/mapayares/miguel-test-repo/git/refs/heads/test-branch");
		deleteCall.addHeader("Accept:", "application/vnd.github.v3+json");
		deleteCall.addHeader("Authorization:", "token " + apiToken);
		deleteCall.addHeader("Content-Type:", MediaType.APPLICATION_JSON);
				
		HttpResponse response = null;
		try {
			response = client.execute(deleteCall);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		HttpEntity entity = response.getEntity();
		
		String content = null;
		if (entity != null) {
			content = CharStreams.toString(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
			EntityUtils.consume(entity); 
		}
		
		HttpClientUtils.closeQuietly(client);
		return content;	
	}
	
	public String createGitHubPullRequest() throws UnsupportedOperationException, IOException {
		logger.info("Creating Pull Request for Miguel Test Repo");
		CloseableHttpClient client = HttpClients.createDefault();
				
		JsonObject json = new JsonObject();
		json.addProperty(StringConstants.TITLE, "testing new pull request");
		json.addProperty("head", "test-branch");
		json.addProperty("base", "master");
		json.addProperty("body", "new pull request!");
		
		HttpPost postCall = new HttpPost(gitHubURL + "/repos/mapayares/miguel-test-repo/pulls");
		postCall.addHeader("Accept:", "application/vnd.github.v3+json");
		postCall.addHeader("Authorization:", "token " + apiToken);
		postCall.addHeader("Content-Type:", MediaType.APPLICATION_JSON);
				
		StringEntity payload = new StringEntity(json.toString(), ContentType.APPLICATION_JSON);
		postCall.setEntity(payload);
		
		HttpResponse response = null;
		try {
			response = client.execute(postCall);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} 
		HttpEntity entity = response.getEntity();
		
		String content = null;
		if (entity != null) {
			content = CharStreams.toString(new InputStreamReader(entity.getContent(), Charsets.UTF_8));
			EntityUtils.consume(entity); 
		}
		
		HttpClientUtils.closeQuietly(client);
		return content;
	}
  
  private Map<String, String> getGitHubData(String gitFilePath) {
		logger.debug("Constructing GitHub Api URL to retrieve content of file for {}", gitFilePath);
 		List<String> gitPathSections = Arrays.asList(gitFilePath.split(backSlash));
		StringJoiner joiner = new StringJoiner(backSlash, gitHubApiUrl, "?").add(GIT_REPOS);
		
		if (!gitPathSections.contains("https:") && !gitPathSections.contains(HttpHost.DEFAULT_SCHEME_NAME + ":")) {
			throw new ProfileServiceException("github url does not contain a recognize application schema ie http, https");
		}
		
		String owner = gitPathSections.get(3);
		String gitRepository = gitPathSections.get(4);
		String gitBranch = gitPathSections.get(6);
		List<String> fileLocation = gitPathSections.subList(7, gitPathSections.size());
		String location = String.join(backSlash, fileLocation);
		
		joiner.add(owner).add(gitRepository).add("contents").add(location);

		Map<String, String> gitData = ImmutableMap.of("git_branch", gitBranch, "api_url", joiner.toString());
		return gitData;
	}

	private String encodeBranchParameter(String gitBranch) {
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("ref", gitBranch));
		String encodedParams = URLEncodedUtils.format(params, Charsets.UTF_8);
		return encodedParams;
	}
	
	private HttpRequestBase addHttpHeaders(HttpRequestBase httpRequest, String account) {
		logger.debug("Adding required headers for github request");
		String apiToken = getGitHubAPIToken(account);
		httpRequest.addHeader(HttpHeaders.CONTENT_TYPE, "application/vnd.github.v3+json");
		httpRequest.addHeader(HttpHeaders.AUTHORIZATION, "token " + apiToken);
		httpRequest.addHeader(HttpHeaders.ACCEPT, "application/vnd.github.3.raw");
		return httpRequest;
	}
	
	private String getGitHubAPIToken(String account) {
		logger.debug("Getting GitHub API Token for Account: {}", account);
		GitHubAccountConfigurations gitHubConfig = null;
		try {
			gitHubConfig = accountService.getGitHubConfiguration(account);
		} catch (Exception e) {
			throw new AccountException("Fail to find github configuration for Account: " + account, e);
		}
		return gitHubConfig.getApiToken();
	}
  
}
