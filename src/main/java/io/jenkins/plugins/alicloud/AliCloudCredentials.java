package io.jenkins.plugins.alicloud;

import com.aliyuncs.DefaultAcsClient;
import com.aliyuncs.edas.model.v20170801.ListApplicationResponse;
import com.aliyuncs.endpoint.EndpointResolver;
import com.aliyuncs.endpoint.ResolveEndpointRequest;
import com.aliyuncs.exceptions.ClientException;
import com.google.common.base.Strings;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.ModelObject;
import hudson.util.FormValidation;
import io.jenkins.plugins.alicloud.edas.EDASService;
import io.jenkins.plugins.alicloud.edas.enumeration.ClusterType;
import io.jenkins.plugins.alicloud.edas.EDASUtils;
import io.jenkins.plugins.alicloud.edas.enumeration.Regions;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class AliCloudCredentials extends AbstractDescribableImpl<AliCloudCredentials> implements ModelObject {

    @Extension
    public static final DescriptorImpl DESCRIPTOR = new DescriptorImpl();

    private final String name;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String endpoint;

    private final static Set<AliCloudCredentials> credentials = new HashSet<>();

    public String getName() {
        return name;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public Regions getRegion() {
        return Regions.DEFAULT_REGION;
    }

    public String getEndpoint() {
        return endpoint;
    }


    @DataBoundConstructor
    public AliCloudCredentials(String name, String accessKeyId, String accessKeySecret, String endpoint) {
        this.name = name;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.endpoint = endpoint;
    }

    @Override
    public String getDisplayName() {
        return name + " : " + accessKeyId;
    }

    public DefaultAcsClient getAcsClient(String regionId) {
        DefaultAcsClient client = null;
        try {
            client = EDASService.getAcsClientByRegonId(this, regionId);
            client.setEndpointResolver(new EndpointResolver() {
                @Override public String resolve(ResolveEndpointRequest request) throws ClientException {
                    if (Strings.isNullOrEmpty(endpoint) || endpoint.endsWith("aliyuncs.com")) {
                        return String.format("edas.%s.aliyuncs.com", regionId);
                    } else {
                        return endpoint;
                    }
                }
            });
        } catch (ClientException e) {
            e.printStackTrace();
        }
        return client;
    }

    public static void configureCredentials(Collection<AliCloudCredentials> toAdd) {
        credentials.clear();
        credentials.addAll(toAdd);
    }

    public static Set<AliCloudCredentials> getCredentials() {
        return credentials;
    }

    public static AliCloudCredentials getCredentialsByString(String credentialsString) {
        Set<AliCloudCredentials> credentials = getCredentials();

        for (AliCloudCredentials credential : credentials) {
            if (credential.getName().equals(credentialsString) || credential.toString().equals(credentialsString)) {
                return credential;
            }
        }

        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AliCloudCredentials)) {
            return false;
        }
        AliCloudCredentials creds = (AliCloudCredentials) o;
        boolean isSame = this.accessKeyId.equals(creds.accessKeyId);
        isSame &= this.name.equals(creds.name);
        return isSame;
    }

    @Override
    public String toString() {
        return name + " : " + accessKeyId;
    }

    @Override
    public int hashCode() {
        return (accessKeyId).hashCode();
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return DESCRIPTOR;
    }


    public final static class DescriptorImpl extends Descriptor<AliCloudCredentials> {

        @Override
        public String getDisplayName() {
            return "Credentials for AliCloud";
        }

        public FormValidation doPingEdas(
            @QueryParameter("accessKeyId") String accessKey,
            @QueryParameter("accessKeySecret") String secretKey,
            @QueryParameter("region") String regionString,
            @QueryParameter("endpoint") String endpoint) {
            if (accessKey == null || secretKey == null) {
                return FormValidation.error("Access key and Secret key cannot be empty");
            }
            AliCloudCredentials credentials = new AliCloudCredentials("", accessKey, secretKey, endpoint);
            Regions region = Enum.valueOf(Regions.class, regionString);
            if (region == null) {
                return FormValidation.error("Missing valid Region");
            }

            try {
                List<ListApplicationResponse.Application> apps = EDASService.getAllApps(credentials.getAcsClient(region.getName()),
                    region.getName(), EDASUtils.ALL_NAMESPACE, ClusterType.ALL_KINDS.value());
            } catch (Exception e) {
                return FormValidation.error(e.getMessage());
            }

            return FormValidation.ok("success");
        }
    }
}
