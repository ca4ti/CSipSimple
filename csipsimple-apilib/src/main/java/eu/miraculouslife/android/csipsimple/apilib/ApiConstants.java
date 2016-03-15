package eu.miraculouslife.android.csipsimple.apilib;

public class ApiConstants {

    public static final String CALL_ENDED_STATUS_INTENT_KEY = "call.ended.status.intent_key";
    public static final String CALL_ENDED_BROADCAST_ACTION = "csipsimple.broadcast.action.call_ended_status";
    public static final String TARGET_NUMBER_INTENT_KEY = "to.call.number.intent.key";
    public static final String TO_CALL_NAME_INTENT_KEY = "to.call.name.intent.key";

    public static final String API_REQUEST_ACTION = "csipsimple.broadcast.action.api_request.action";

    public static final String REQUEST_TYPE_INTENT_KEY = "csipsimple.api.request_type";
    public static final int REQUEST_TYPE_MAKE_CALL = 100;
    public static final int REQUEST_TYPE_OPEN_SETTINGS_PAGE = 200;
    public static final int REQUEST_TYPE_SEND_MESSAGE = 300;
    public static final String MESSAGE_INTENT_KEY = "message.intent.key";
}