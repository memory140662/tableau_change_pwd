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
        CONFIG_KEY_NAME.put("-ndp", "newDbPassword");
        CONFIG_KEY_NAME.put("-t", "type");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("version: 2018/02/09");
        int res = 0;

        RestApiUtils utils = null;
        TableauCredentialsType credential = null;
        try {
            Map<String, String> config = new App().getConfig(args);

            String server = config.get("server");
            String username = config.get("username");
            String password = config.get("password");
            String dbNewPassword = config.get("newDbPassword");
            String type = config.get("type");

            if (username == null || password == null || server == null) throw new Exception("Username, Password or Server is null.");
            utils = RestApiUtils.getInstance(server);
            credential = utils.invokeSignIn(username, password, (server.contains("localhost")) ? "" : server);
            Map<String, Object> datasources = utils.invokeQueryDatasources(credential, credential.getSite().getId());
            Object tmp;
            for (String datasourceKey: datasources.keySet()) {
                List<Map<String, Object>> datasource = (List<Map<String, Object>>) datasources.get(datasourceKey);
                for (Map<String, Object> data: datasource) {
                    if (isTypeNotEqual(type, data)) continue;
                    System.out.println(String.format("Datasource Name: %s, Type: %s", data.get("name"), data.get("type")));
                    Map<String, Object> connections = utils.invokeQueryDatasourceConnections(credential, credential.getSite().getId(), (String) data.get("id"));
                    for (String connectionKey: connections.keySet()) {
                        List<Map<String, Object>> connection = (List<Map<String, Object>>) connections.get(connectionKey);
                        for (Map<String, Object> conn: connection) {
                            System.out.println(String.format("\tConnection User Name: %s", conn.get("userName")));
                            if (dbNewPassword != null && !dbNewPassword.trim().isEmpty()) {
                                tmp = utils.invokeUpdateDatasourceConnection(
                                        credential,
                                        credential.getSite().getId(),
                                        (String) data.get("id"),
                                        (String) conn.get("id"),
                                        dbNewPassword
                                );
                                if (tmp == null) {
                                    System.out.println("Update datasource connection failed.");
                                }
                            }
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
                if ((index + 1) < args.length && !args[index + 1].startsWith("-")) {
                    if (CONFIG_KEY_NAME.get(arg) == null) {
                        throw new RuntimeException("不知名的指令：".concat(arg));
                    }
                    config.put(CONFIG_KEY_NAME.get(arg), (++index < args.length) ? args[index] : null);
                }
            }
        }
        return config;
    }
}
