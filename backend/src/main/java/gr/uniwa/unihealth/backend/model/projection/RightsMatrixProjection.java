package gr.uniwa.unihealth.backend.model.projection;

import gr.uniwa.unihealth.backend.model.enums.Permission;
import gr.uniwa.unihealth.backend.model.enums.UserStatus;

/**
 * Spring Data projection for efficiently loading the rights matrix of a user.
 *
 * @author European Dynamics SA
 */
public interface RightsMatrixProjection {

  String getUserId();

  UserStatus getUserStatus();

  Permission getGlobalPermission();
}
