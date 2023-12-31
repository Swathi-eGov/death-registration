package digit.service;

import digit.config.DTRConfiguration;
import digit.util.UserUtil;
import digit.web.models.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.egov.common.contract.request.RequestInfo;
import org.egov.tracer.model.CustomException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
public class UserService {
    private UserUtil userUtils;

    private DTRConfiguration config;

    @Autowired
    public UserService(UserUtil userUtils, DTRConfiguration config) {
        this.userUtils = userUtils;
        this.config = config;
    }

    /**
     * Calls user service to enrich user from search or upsert user
     * @paramrequest
     */
    public void callUserService(DeathRegistrationRequest request){
        request.getDeathRegistrationApplications().forEach(application -> {
            if(application.getApplicant()!=null) {
                if (!(application.getApplicant().getId() == null))
                    enrichUser(application, request.getRequestInfo());
                else {
                    User user = createApplicant(application);
                    User applicantIdAndUuid = upsertUser(user, request.getRequestInfo());
                    application.getApplicant().setId(applicantIdAndUuid.getId());
                    application.getApplicant().setUuid(applicantIdAndUuid.getUuid());
                }
            }
        });
    }
    private User upsertUser(User user, RequestInfo requestInfo){
        String tenantId = user.getTenantId();
        User userServiceResponse = null;
        // Search on mobile number as user name
        UserDetailResponse userDetailResponse = searchUser(userUtils.getStateLevelTenant(tenantId),null, user.getUserName());
        if (!userDetailResponse.getUser().isEmpty()) {
            User userFromSearch = userDetailResponse.getUser().get(0);
            log.info(userFromSearch.toString());
            if(!user.getUserName().equalsIgnoreCase(userFromSearch.getUserName())){
                userServiceResponse = updateUser(requestInfo,user,userFromSearch);
            }
            else userServiceResponse = userDetailResponse.getUser().get(0);
        }
        else {
            userServiceResponse = createUser(requestInfo,tenantId,user);
        }
        return userServiceResponse;
    }

    private User createApplicant(DeathRegistrationApplication application){
        Applicant applicant = application.getApplicant();
        User user = User.builder().userName(applicant.getUserName())
                .name(applicant.getName())
                .mobileNumber(applicant.getMobileNumber())
                .emailId(applicant.getEmailId())
                .tenantId(applicant.getTenantId())
                .type(applicant.getType())
                .roles(applicant.getRoles())
                .build();
        return user;
    }

    private User createUser(RequestInfo requestInfo, String tenantId, User userInfo) {
        userUtils.addUserDefaultFields(userInfo.getUserName(),tenantId, userInfo);
        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserCreateEndpoint());
        CreateUserRequest user = new CreateUserRequest(requestInfo, userInfo);
        log.info(user.getUser().toString());
        UserDetailResponse userDetailResponse = userUtils.userCall(user, uri);
        return userDetailResponse.getUser().get(0);

    }


    private void enrichUser(DeathRegistrationApplication application, RequestInfo requestInfo){
        String accountIdApplicant = application.getApplicant().getUuid();
        String tenantId = application.getTenantId();
        UserDetailResponse userDetailResponse = searchUser(userUtils.getStateLevelTenant(tenantId),accountIdApplicant,application.getApplicant().getUserName());
        if(userDetailResponse.getUser().isEmpty())
            throw new CustomException("INVALID_ACCOUNTID","No user exist for the given accountId");
        else application.getApplicant().setUuid(userDetailResponse.getUser().get(0).getUuid());

    }
    /**
     * Creates the user from the given userInfo by calling user service
     *
     * @param requestInfo
     * @param tenantId
     * @param userInfo
     * @return
     */
    /**
     * Updates the given user by calling user service
     //     * @param requestInfo
     //     * @param user
     //     * @param userFromSearch
     * @return
     */
    public UserDetailResponse searchUser(String stateLevelTenant, String accountId, String userName){
        UserSearchRequest userSearchRequest =new UserSearchRequest();
        userSearchRequest.setActive(true);
        userSearchRequest.setType("CITIZEN");
        userSearchRequest.setTenantId(stateLevelTenant);
        if(accountId==null && userName==null)
            return null;
        if(accountId!=null)
            userSearchRequest.setUuid(Collections.singletonList(accountId));
        if(userName!=null)
            userSearchRequest.setUserName(userName);
        StringBuilder uri = new StringBuilder(config.getUserHost()).append(config.getUserSearchEndpoint());
        return userUtils.userCall(userSearchRequest,uri);
    }
    private User updateUser(RequestInfo requestInfo,User user,User userFromSearch) {
        userFromSearch.setUserName(user.getUserName());
        userFromSearch.setActive(true);
        StringBuilder uri = new StringBuilder(config.getUserHost())
                .append(config.getUserContextPath())
                .append(config.getUserUpdateEndpoint());
        UserDetailResponse userDetailResponse = userUtils.userCall(new CreateUserRequest(requestInfo, userFromSearch), uri);
        return userDetailResponse.getUser().get(0);
    }
}