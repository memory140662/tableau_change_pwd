package tw.com.techlink;

import rest.bindings.TableauCredentialsType;
import rest.util.RestApiUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class App {

    private static final Map<String, String> CONFIG_KEY_NAME = new HashMap<>();
    static {
        CONFIG_KEY_NAME.put("-s", "server");
        CONFIG_KEY_NAME.put("-p", "password");
        CONFIG_KEY_NAME.put("-u", "username");
        CONFIG_KEY_NAME.put("-dnp", "dbNewPassword");
        CONFIG_KEY_NAME.put("-t", "type");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("version: 2018/02/07");
        int res = 0;
        Map<String, String> config = new App().getConfig(args);

        String server = config.get("server");
        String username = config.get("username");
        String password = config.get("password");
        String dbNewPassword = config.get("dbNewPassword");
        String type = config.get("type");

        RestApiUtils utils = null;
        TableauCredentialsType credential = null;
        try {
            utils = RestApiUtils.getInstance(server);
            credential = utils.invokeSignIn(username, password, server);
            Map<String, Object> datasources = utils.invokeQueryDatasources(credential, credential.getSite().getId());
            for (String datasourceKey: datasources.keySet()) {
                List<Map<String, Object>> datasource = (List<Map<String, Object>>) datasources.get(datasourceKey);
                for (Map<String, Object> data: datasource) {
                    if (isTypeNotEqual(type, data)) continue;
                    Map<String, Object> connections = utils.invokeQueryDatasourceConnections(credential, credential.getSite().getId(), (String) data.get("id"));
                    for (String connectionKey: connections.keySet()) {
                        List<Map<String, Object>> connection = (List<Map<String, Object>>) connections.get(connectionKey);
                        for (Map<String, Object> conn: connection) {
                            utils.invokeUpdateDatasourceConnection(
                                    credential,
                                    credential.getSite().getId(),
                                    (String) data.get("id"),
                                    (String) conn.get("id"),
                                    dbNewPassword
                            );
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            res = 1;
        } finally {
            if (credential != null) {
                utils.invokeSignOut(credential);
            }
        }
        System.exit(res);
    }

    private static boolean isTypeNotEqual(String type, Map<String, Object> data) {
        return type != null && !type.trim().isEmpty() && !type.equalsIgnoreCase((String) data.get("type"));
    }

    private Map<String, String> getConfig(String ...args) {
        Map<String, String> config = new HashMap<>();
        String arg;
        for (int index = 0; index < args.length; index++) {
            arg = args[index];
            if (arg.startsWith("-")) {
                if ((index + 1) > args.length && !args[index + 1].startsWith("-")) {
                    config.put(CONFIG_KEY_NAME.get(arg), (++index < args.length) ? args[index] : null);
                }
            }
        }
        return config;
    }
}
