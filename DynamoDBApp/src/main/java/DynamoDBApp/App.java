package DynamoDBApp;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Locale;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.google.gson.Gson;

public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private static final AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
    private static final DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

    public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) {
        String httpMethod = input.getHttpMethod();
        Map<String, List<String>> inputParams = input.getMultiValueQueryStringParameters();
        String output;
        int statusCode;

        switch (httpMethod) {
            case "GET":
                if (inputParams != null && inputParams.get("dateOfWeek") != null) {
                    String dateEpoch = inputParams.get("dateOfWeek").get(0);
                    if (dateEpoch.endsWith("?")) {
                        dateEpoch = dateEpoch.substring(0, dateEpoch.length() - 1);
                    }
                    output = getUsersFromDate(dateEpoch);
                } else {
                    output = getUsers();
                }
                statusCode = 200;
                break;
            case "POST":
                output = createUser(input.getBody());
                statusCode = 201;
                break;
            case "PUT":
                output = updateUser(input.getBody());
                statusCode = 200;
                break;
            case "DELETE":
                output = deleteUser(input.getBody());
                statusCode = 200;
                break;
            default:
                output = "Invalid HTTP Method";
                statusCode = 400;
                break;
        }

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent()
                .withStatusCode(statusCode)
                .withHeaders(headers)
                .withBody(output);

        return response;
    }

    private String getUsers() {
        List<User> users = dynamoDBMapper.scan(User.class, new DynamoDBScanExpression());
        return new Gson().toJson(users);
    }

    private String getUsersFromDate(String date) {
        System.out.println("input date : " + date);
        Calendar calendar = new Calendar.Builder().setFields(Calendar.SECOND, Integer.parseInt(date)).build();
        // calendar.set(Calendar.MILLISECOND, Integer.parseInt(date));
        int year = calendar.get(Calendar.YEAR);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);

        Map<String, AttributeValue> eav = new HashMap<String, AttributeValue>();
        eav.put(":val1", new AttributeValue().withS("date_" + year + "_" + weekOfYear));

        DynamoDBQueryExpression<User> queryExpression = new DynamoDBQueryExpression<User>()
                .withKeyConditionExpression("id=:val1")
                .withExpressionAttributeValues(eav);

        List<User> users = dynamoDBMapper.query(User.class, queryExpression);
        return new Gson().toJson(users);
    }

    private String createUser(String requestBody) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        User user = new Gson().fromJson(requestBody, User.class);
        user.setId("date_" + year + "_" + weekOfYear);
        user.setDate(calendar.getTimeInMillis());
        dynamoDBMapper.save(user);
        return "User created: " + user.getId();
    }

    private String updateUser(String requestBody) {
        User updatedUser = new Gson().fromJson(requestBody, User.class);
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int weekOfYear = calendar.get(Calendar.WEEK_OF_YEAR);
        updatedUser.setId("date_" + year + "_" + weekOfYear);
        String hashKey = updatedUser.getId();
        String rangeKey = updatedUser.getEmail();
        User existingUser = dynamoDBMapper.load(User.class, hashKey, rangeKey);

        if (existingUser != null) {
            existingUser.setScore(updatedUser.getScore());
            existingUser.setLevel(updatedUser.getLevel());
            existingUser.setName(updatedUser.getName());
            existingUser.setEmail(updatedUser.getEmail());
            existingUser.setDate(calendar.getTimeInMillis());
            dynamoDBMapper.save(existingUser);
            return "User updated: " + existingUser.getId();
        } else {
            createUser(requestBody);
            return "User recreated";
        }
    }

    private String deleteUser(String requestBody) {
        User userToDelete = new Gson().fromJson(requestBody, User.class);
        User existingUser = dynamoDBMapper.load(User.class, userToDelete.getId());
        if (existingUser != null) {
            dynamoDBMapper.delete(existingUser);
            return "User deleted: " + existingUser.getId();
        } else {
            return "User not found";
        }
    }
}

// import java.util.HashMap;
// import java.util.Map;

// import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
// import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
// import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
// import com.amazonaws.services.lambda.runtime.Context;
// import com.amazonaws.services.lambda.runtime.RequestHandler;
// import
// com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
// import
// com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

// public class App1 implements RequestHandler<APIGatewayProxyRequestEvent,
// APIGatewayProxyResponseEvent> {
// public APIGatewayProxyResponseEvent handleRequest(final
// APIGatewayProxyRequestEvent input, final Context context) {

// AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
// DynamoDBMapper dynamoDBMapper = new DynamoDBMapper(client);

// Map<String, String> headers = new HashMap<>();
// headers.put("Content-Type", "application/json");
// headers.put("X-Custom-Header", "application/json");
// APIGatewayProxyResponseEvent response = new
// APIGatewayProxyResponseEvent().withHeaders(headers);
// String output = String.format("{ \"message\": \"hello world\", \"location\":
// \"%s\" }", "pageContents");

// User user = new User();
// user.setEmpId("EMP001");
// user.setName("John Doe");
// user.setEmail("johndoe@example.com");

// dynamoDBMapper.save(user);
// context.getLogger().log("Successfully added new Employee");

// return response.withStatusCode(200).withBody(output);
// }
// }
