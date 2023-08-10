package digit.repository.rowmapper;

import digit.web.models.Applicant;
import digit.web.models.AuditDetails;
import digit.web.models.Address;
import digit.web.models.DeathRegistrationApplication;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Component
public class DeathApplicationRowMapper implements ResultSetExtractor<List<DeathRegistrationApplication>> {
    public List<DeathRegistrationApplication> extractData(ResultSet rs) throws SQLException, DataAccessException {
        Map<String,DeathRegistrationApplication> DeathRegistrationApplicationMap = new LinkedHashMap<>();

        while (rs.next()){
            String uuid = rs.getString("dapplicationnumber");
            DeathRegistrationApplication DeathRegistrationApplication = DeathRegistrationApplicationMap.get(uuid);

            if(DeathRegistrationApplication == null) {

                Long lastModifiedTime = rs.getLong("dlastModifiedTime");
                if (rs.wasNull()) {
                    lastModifiedTime = null;
                }


//                Applicant father = Applicant.builder().id(rs.getString("dfatherid")).build();
//                Applicant mother = Applicant.builder().id(rs.getString("dmotherid")).build();

                AuditDetails auditdetails = AuditDetails.builder()
                        .createdBy(rs.getString("dcreatedBy"))
                        .createdTime(rs.getLong("dcreatedTime"))
                        .lastModifiedBy(rs.getString("dlastModifiedBy"))
                        .lastModifiedTime(lastModifiedTime)
                        .build();

                DeathRegistrationApplication = DeathRegistrationApplication.builder()
                        .applicationNumber(rs.getString("dapplicationnumber"))
                        .tenantId(rs.getString("dtenantid"))
                        .id(rs.getString("did"))
                        .deceasedFirstName(rs.getString("ddeceasedfirstname"))
                        .deceasedLastName(rs.getString("ddeceasedlastname"))
//                        .fatherOfApplicant(father)
//                        .motherOfApplicant(mother)
                        .placeOfDeath(rs.getString("dplaceofDeath"))
                        .timeOfDeath(rs.getInt("dtimeofDeath"))
                        .auditDetails(auditdetails)
                        .build();
            }
            addChildrenToProperty(rs, DeathRegistrationApplication);
            DeathRegistrationApplicationMap.put(uuid, DeathRegistrationApplication);
        }
        return new ArrayList<>(DeathRegistrationApplicationMap.values());
    }

    private void addChildrenToProperty(ResultSet rs, DeathRegistrationApplication DeathRegistrationApplication)
            throws SQLException {
        addAddressToApplication(rs, DeathRegistrationApplication);
    }

    private void addAddressToApplication(ResultSet rs, DeathRegistrationApplication DeathRegistrationApplication) throws SQLException {
        Address address = Address.builder()
               // .id(rs.getString("aid"))
               // .registrationId(rs.getString("aregistrationid"))
                .registrationId("aregistrationid")
                .tenantId(rs.getString("atenantid"))
               // .doorNo(rs.getString("adoorno"))
                .latitude(rs.getDouble("alatitude"))
                .longitude(rs.getDouble("alongitude"))
                //.buildingName(rs.getString("abuildingname"))
                .addressId(rs.getString("aaddressid"))
                .addressNumber(rs.getString("aaddressnumber"))
             //   .type(rs.getString("atype"))
                .addressLine1(rs.getString("aaddressline1"))
                .addressLine2(rs.getString("aaddressline2"))
                .landmark(rs.getString("alandmark"))
               // .street(rs.getString("astreet"))
                .city(rs.getString("acity"))
                .pincode(rs.getString("apincode"))
                .detail("adetail")
                .build();

        DeathRegistrationApplication.setAddressOfDeceased(address);

    }

}
