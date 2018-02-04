package tw.com.techlink;

import rest.bindings.TableauCredentialsType;
import rest.util.RestApiUtils;

import java.util.HashMap;
import java.util.Map;

public class App {

    private static final Map<String, String> CONFIG_KEY_NAME = new HashMap<>();
    static {
        CONFIG_KEY_NAME.put("-s", "server");
        CONFIG_KEY_NAME.put("-p", "password");
        CONFIG_KEY_NAME.put("-u", "username");
        CONFIG_KEY_NAME.put("-du", "dbUsername");
        CONFIG_KEY_NAME.put("-dnp", "dbNewPassword");
        CONFIG_KEY_NAME.put("-dn", "datasourceName");
    }

    public static void main(String[] args) {
        System.out.println("version: 2018/02/04");
        int res = 0;
        Map<String, String> config = new App().getConfig(args);

        String server = config.get("server");
        String username = config.get("username");
        String password = config.get("password");
        String dbUsername = config.get("dbUsername");
        String dbNewPassword = config.get("dbNewPassword");
        String datasourceName = config.get("datasourceName");


        RestApiUtils utils = RestApiUtils.getInstance(server);
        TableauCredentialsType credential = null;
        try {
            credential = utils.invokeSignIn(username, password, server);
            Map<String, Object> datasources = utils.invokeQueryDatasources(credential, credential.getSite().getId());
            for (String datasourceKey: datasources.keySet()) {
                Map<String, Object> datasource = (Map<String, Object>) datasources.get(datasourceKey);
                Map<String, Object> connections = utils.invokeQueryDatasourceConnections(credential, credential.getSite().getId(), (String) datasource.get("id"));
                for (String connectionKey: connections.keySet()) {
                    Map<String, Object> connection = (Map<String, Object>) connections.get(connectionKey);
                    utils.invokeUpdateDatasourceConnection(
                            credential,
                            credential.getSite().getId(),
                            (String) datasource.get("id"),
                            (String) connection.get("id"),
                            dbUsername,
                            dbNewPassword
                        );
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
