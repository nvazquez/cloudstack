package org.apache.cloudstack.api.command.user.template;

import com.cloud.exception.ConcurrentOperationException;
import com.cloud.exception.InsufficientCapacityException;
import com.cloud.exception.NetworkRuleConflictException;
import com.cloud.exception.ResourceAllocationException;
import com.cloud.exception.ResourceUnavailableException;
import org.apache.cloudstack.acl.RoleType;
import org.apache.cloudstack.api.APICommand;
import org.apache.cloudstack.api.ApiConstants;
import org.apache.cloudstack.api.BaseCmd;
import org.apache.cloudstack.api.BaseListCmd;
import org.apache.cloudstack.api.Parameter;
import org.apache.cloudstack.api.ServerApiException;
import org.apache.cloudstack.api.response.ListResponse;
import org.apache.cloudstack.api.response.TemplateEulaResponse;
import org.apache.cloudstack.api.response.TemplateResponse;
import org.apache.cloudstack.context.CallContext;

@APICommand(name = ListTemplateEulaSections.APINAME,
        description = "List EULA sections for the template if available.",
        responseObject = TemplateEulaResponse.class,
        authorized = {RoleType.Admin, RoleType.DomainAdmin, RoleType.ResourceAdmin, RoleType.User})
public class ListTemplateEulaSections extends BaseListCmd {

    public static final String APINAME = "listTemplateEulaSections";

    @Parameter(name = ApiConstants.ID, type = BaseCmd.CommandType.UUID, entityType = TemplateResponse.class,
            description = "the template ID", required = true)
    private Long templateId;

    public Long getTemplateId() {
        return templateId;
    }

    @Override
    public void execute() throws ResourceUnavailableException, InsufficientCapacityException, ServerApiException, ConcurrentOperationException, ResourceAllocationException, NetworkRuleConflictException {
        ListResponse<TemplateEulaResponse> response = _queryService.listTemplateEulaSections(this);
        response.setResponseName(getCommandName());
        setResponseObject(response);
    }

    @Override
    public String getCommandName() {
        return APINAME.toLowerCase() + BaseCmd.RESPONSE_SUFFIX;
    }

    @Override
    public long getEntityOwnerId() {
        return CallContext.current().getCallingAccount().getId();
    }
}
