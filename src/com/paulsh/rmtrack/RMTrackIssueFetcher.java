package com.paulsh.rmtrack;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.StreamUtil;
import jetbrains.buildServer.http.HttpUtil;
import jetbrains.buildServer.issueTracker.AbstractIssueFetcher;
import jetbrains.buildServer.issueTracker.IssueData;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.CollectionsUtil;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.util.DateUtil;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import sun.misc.BASE64Encoder;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RMTrackIssueFetcher extends AbstractIssueFetcher {
    private static final Logger LOG = Loggers.SERVER; // Logger.getInstance(RMTrackIssueFetcher.class.getName());

    public static final String ASSIGNED_TO_FIELD = "Assigned To";
    public static final String CUSTOMER_FIELD = "Customer";

    private Pattern myPattern;

    public RMTrackIssueFetcher(@NotNull jetbrains.buildServer.util.cache.EhCacheUtil cacheUtil) {
        super(cacheUtil);
    }


    public void setPattern(final Pattern _myPattern) {
        myPattern = _myPattern;
    }

    public class RMTIssueFetchFunction implements AbstractIssueFetcher.FetchFunction {

        private String myServerUrl;
        private String myId;
        private UsernamePasswordCredentials myCredentials;

        public RMTIssueFetchFunction(final String serverUrl, final String id, final Credentials credentials) {
            if (serverUrl.length() == 0) {
                throw new IllegalArgumentException(String.format("ServerUrl cannot be empty"));
            }
            if (!(credentials instanceof UsernamePasswordCredentials)) {
                throw new IllegalArgumentException(String.format("Credentials must be of UsernamePasswordCredentials type"));
            }

            this.myServerUrl = serverUrl;
            this.myId = id;
            this.myCredentials = (UsernamePasswordCredentials)credentials;
        }

        @NotNull
        public IssueData fetch() throws Exception {
            String url = getApiUrl(myServerUrl, myId);
            LOG.debug(String.format("Fetching issue data from %s", url));
            try {
                InputStream xml = fetchIssueDetailsXml(url, myCredentials);
                IssueData result = parseIssue(xml);
                LOG.debug("IssueData: " + result.toString());
                return result;
            }   catch (Exception e) {
                LOG.error(e);
                throw new RuntimeException("Error fetching issue data", e);
            }
        }

        private IssueData parseIssue(InputStream xml) {
            try {
                InputStreamReader reader = new InputStreamReader(xml, "CP1252");
                Element issue = getSaxBuilder(false).build(reader).getRootElement();

                Element fields = issue.getChild("Fields");
                if (fields == null) {
                  throw new RuntimeException(String.format("Invalid XML for issue '%s' on '%s'.", myId, myServerUrl));
                }
                String summary = getChildContent(fields, "Summary");
                String state = getChildContent(fields, "StatusCode");
                String resolution = getChildContent(fields, "ResolutionCode");
                String priority = getChildContent(fields, "PriorityCode");
                String severity = getChildContent(fields, "SeverityCode");
                String errorType = getChildContent(fields, "ErrorTypeCode");
                String assignedTo = getChildContent(fields, "AssignedToUserId");
                String customer = getChildContent(fields, "Customer");
                String url = getUrl(myServerUrl, myId);
                boolean isResolved = state != null && state.equalsIgnoreCase("Closed");
                boolean isFeatureRequest = errorType != null && errorType.equalsIgnoreCase("Feature enhancement");
                
                if (resolution != null && resolution.length() > 0) {
                    state = state + " - " + resolution;
                }

                return new IssueData(myId,
                        CollectionsUtil.asMap(
                                IssueData.SUMMARY_FIELD, summary,
                                IssueData.STATE_FIELD, state,
                                IssueData.TYPE_FIELD, errorType,
                                IssueData.PRIORITY_FIELD, priority,
                                IssueData.SEVERITY_FIELD, severity,
                                ASSIGNED_TO_FIELD, assignedTo,
                                CUSTOMER_FIELD, customer),
                        isResolved,
                        isFeatureRequest,
                        url);

            } catch (JDOMException e) {
                LOG.error(e);
                throw new RuntimeException(String.format("Error parsing XML for issue '%s' on '%s'. %s", myId, myServerUrl, e.getMessage()));
            } catch (IOException e) {
                LOG.error(e);
                throw new RuntimeException(String.format("Error reading XML for issue '%s' on '%s'. %s", myId, myServerUrl, e.getMessage()));
            }
        }
        @Nullable
        protected InputStream fetchIssueDetailsXml(@NotNull String url, @NotNull UsernamePasswordCredentials credentials)
                throws IOException, InvalidKeyException, NoSuchAlgorithmException {
            HttpClient httpClient = HttpUtil.createHttpClient(120, new URL(url), credentials);
            GetMethod get = new GetMethod(url);
            get.addRequestHeader("x-rmtrack-date", DateUtil.formatDate(new Date(), DateUtil.PATTERN_RFC1123));

            StringBuilder b = new StringBuilder();
            b.append("GET");
            b.append("\n");
            b.append(new URL(url).getPath());
            b.append("\n");
            b.append(get.getRequestHeader("x-rmtrack-date").getValue());
            b.append("\n");

            String accessKey = credentials.getUserName();
            String secretKey = credentials.getPassword();

            BASE64Encoder base64 = new BASE64Encoder();
            String signature = base64.encode(calculateHMAC(b.toString().getBytes(), secretKey.getBytes()));
            get.addRequestHeader("Authorization", "RMT " + accessKey + ":" + signature);

            int code = httpClient.executeMethod(get);

            if (code < 200 || code >= 300) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("HTTP Response body:\n" + StreamUtil.readText(get.getResponseBodyAsStream()));
                }
                throw new RuntimeException("Failed to fetch issue details for \"" + url + "\", HTTP response code: " + code);
            }

            LOG.debug("HTTP response: " + code + ", length: " + get.getResponseContentLength());
            return get.getResponseBodyAsStream();
        }

        byte[] calculateHMAC(byte[] data, byte[] secret) throws NoSuchAlgorithmException, InvalidKeyException {
            SecretKey key = new SecretKeySpec(secret, "HmacSHA1");
            Mac m = Mac.getInstance("HmacSHA1");
            m.init(key);
            return m.doFinal(data);
        }

        @NotNull
          private SAXBuilder getSaxBuilder(final boolean validate) {
            SAXBuilder builder = new SAXBuilder(validate);
            builder.setFeature("http://xml.org/sax/features/namespaces", true);
            builder.setFeature("http://xml.org/sax/features/namespace-prefixes", true);
            builder.setEntityResolver(new DefaultHandler() {
              @Override
              public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
                String dtdFileName = new File(systemId).getName();
                InputStream dtdStream = getClass().getClassLoader().getResourceAsStream(dtdFileName);
                if (dtdStream != null) {
                  return new InputSource(dtdStream);
                }

                return super.resolveEntity(publicId, systemId);
              }
            });

            return builder;
          }
    }

    @NotNull
    public IssueData getIssue(@NotNull String host, @NotNull String id, @Nullable Credentials credentials) throws Exception {
        String url = getUrl(host, id);
        RMTIssueFetchFunction fetchFunction = new RMTIssueFetchFunction(host, id, credentials);
        return getFromCacheOrFetch(url, fetchFunction);
    }

    @NotNull
    public String getUrl(@NotNull String serverUrl, @NotNull String id) {
        return String.format("%sNonAdmin/Issues/IssueDetails.aspx?IssueId=%s", serverUrl, getRealId(id));
    }

    @NotNull
    private String getApiUrl(@NotNull String serverUrl, @NotNull String id) {
        return String.format("%srmtrackapi/IssueDetails.ashx?Include=Fields&IssueId=%s", serverUrl, getRealId(id));
    }

    private String getRealId(@NotNull String id) {
        Matcher matcher = myPattern.matcher(id);
        String realId = id;
        if (matcher.find()) {
            realId = matcher.group(1);
        }
        return realId;
    }
}
