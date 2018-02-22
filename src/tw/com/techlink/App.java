package tw.com.techlink;

import rest.bindings.SiteListType;
import rest.bindings.SiteType;
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
        CONFIG_KEY_NAME.put("-cu", "contentUrl");
    }

    @SuppressWarnings("unchecked")
    public static void main(String[] args) {
        System.out.println("version: 2018/02/22");
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
            String contentUrl = (config.get("contentUrl") == null)? "": config.get("contentUrl");
            TableauCredentialsType siteCredential;
            if (isAnyNull(username, password, server)) throw new Exception("Username, Password or Server is null.");
            utils = RestApiUtils.getInstance(server);
            credential = utils.invokeSignIn(username, password, contentUrl);
            if (isAnyNull(credential)) throw new Exception("Login failed.");
            siteCredential = credential;
            SiteListType siteListType = utils.invokeQuerySites(credential);
            if (isAnyNull(siteListType)) throw new Exception("Site not found.");
            for(SiteType siteType: siteListType.getSite()) {
                if (isAnyNull(siteType, siteCredential)) continue;
                if (!siteType.getId().equals(credential.getSite().getId())) {
                    siteCredential = utils.invokeSwitchSite(siteCredential, siteType.getId(), siteType.getContentUrl());
                    if (isAnyNull(siteCredential)) {
                        System.out.println("Switch site to ".concat(siteType.getId()).concat(" failed."));
                        continue;
                    }
                }
                Map<String, Object> datasources = utils.invokeQueryDatasources(siteCredential, siteType.getId());
                Object tmp;
                if (isAnyNull(datasources)) continue;
                for (String datasourceKey : datasources.keySet()) {
                    List<Map<String, Object>> datasource = (List<Map<String, Object>>) datasources.get(datasourceKey);
                    if (isAnyNull(datasource)) continue;
                    for (Map<String, Object> data : datasource) {
                        if (isTypeNotEqual(type, data)) continue;
                        System.out.println(String.format("Datasource Name: %s, Type: %s", data.get("name"), data.get("type")));
                        Map<String, Object> connections = utils.invokeQueryDatasourceConnections(siteCredential, siteType.getId(), (String) data.get("id"));
                        if (isAnyNull(connections)) continue;
                        for (String connectionKey : connections.keySet()) {
                            List<Map<String, Object>> connection = (List<Map<String, Object>>) connections.get(connectionKey);
                            if (isAnyNull(connection)) continue;
                            for (Map<String, Object> conn : connection) {
                                if (isAnyNull(conn)) continue;
                                System.out.println(String.format("\tConnection User Name: %s", conn.get("userName")));
                                if (!isAnyNull(dbNewPassword) && !dbNewPassword.trim().isEmpty()) {
                                    tmp = utils.invokeUpdateDatasourceConnection(
                                            siteCredential,
                                            siteType.getId(),
                                            (String) data.get("id"),
                                            (String) conn.get("id"),
                                            dbNewPassword
                                    );
                                    if (isAnyNull(tmp)) {
                                        System.out.println("Update datasource connection failed.");
                                    }
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
        return !isAnyNull(type) && !type.trim().isEmpty() && !type.equalsIgnoreCase((String) data.get("type"));
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

    private static boolean isAnyNull(Object... objs) {
        boolean isNull = false;
        for (Object obj: objs) {
            if (obj == null) {
                isNull = true;
                break;
            }
        }
        return isNull;
    }
}
