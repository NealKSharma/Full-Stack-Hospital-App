package backend;

/**
 * Controller used to showcase what happens when an exception is thrown
 *
 * @author Neal Kaushik Sharma
 * @author Devank Uppal
 */

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

// OpenAPI imports
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
class ExceptionController {

    @RequestMapping(method = RequestMethod.GET, path = "/oops")
    @Operation(summary = "Trigger a test exception")
    @ApiResponses({
            @ApiResponse(responseCode = "500", description = "Exception thrown intentionally for testing"),
    })
    public String triggerException() {
        throw new RuntimeException("Check to see what happens when an exception is thrown");
    }

}
