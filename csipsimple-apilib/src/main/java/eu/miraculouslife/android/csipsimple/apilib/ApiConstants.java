package eu.miraculouslife.android.csipsimple.apilib;

public class ApiConstants {

    public static final String API_REQUEST_ACTION = "csipsimple.broadcast.action.api_request.action";
    public static final String REQUEST_TYPE_INTENT_KEY = "csipsimple.api.request_type";
    public static final int REQUEST_TYPE_MAKE_CALL = 101;
    public static final int REQUEST_TYPE_OPEN_SETTINGS_PAGE = 102;
    public static final int REQUEST_TYPE_SEND_MESSAGE = 103;
    public static final int REQUEST_TYPE_INSTALLATION_CHECK = 104;
    public static final int REQUEST_TYPE_VERSION_REQUEST = 105;
    public static final int REQUEST_TYPE_UPDATE_REGISTRATION = 106;


    public static final String API_RESPONSE_BROADCAST_ACTION = "csipsimple.broadcast.action.api_response";
    public static final String API_RESPONSE_TYPE_INTENT_KEY = "csipsimple.api.responsetype.intent_key";

    public static final int API_RESPONSE_TYPE_CALL_ENDED = 201;
    public static final int API_RESPONSE_TYPE_CALL_CANCELLED = 202;
    public static final int API_RESPONSE_TYPE_INSTALLATION_CHECK = 210;
    public static final int API_RESPONSE_TYPE_CALL_CONNECTED = 220;
    public static final int API_RESPONSE_TYPE_CSIPSIMPLE_VERSION = 230;
    public static final int API_RESPONSE_TYPE_STARTING_CALL = 301;

    public static final String CONTACT_NUMBER_INTENT_KEY = "to.call.number.intent.key";
    public static final String CONTACT_NAME_INTENT_KEY = "to.call.name.intent.key";
    public static final String MESSAGE_INTENT_KEY = "message.intent.key";
    public static final String IS_CALL_INCOMING_INTENT_KEY = "calltype.isincoming.intent.key";
    public static final String CALL_CONNECTED_CALLEE_INTENT_KEY = "call.connected.callee_name.intent.key";
    public static final String CSIPIMPLE_VERSION_INTENT_KEY = "csipsimple.api.responsetype.csipsimple_version";
    public static final String APPNAME = "ML SIP client";
    public static final String CONTACT_DOMAIN_INTENT_KEY = "contact.domain.intent.key";
    public static final String CONTACT_PASS_INTENT_KEY = "contact.pass.intent.key";
}