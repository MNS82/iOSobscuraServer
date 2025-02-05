package ca.litten.ios_obscura_server.backend;

import ca.litten.ios_obscura_server.parser.Binary;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AppList {
    private static final ArrayList<App> apps = new ArrayList<>();

    public static void loadAppDatabaseFile(File file) {
        loadAppDatabaseFile(file, false, false);
    }
    
    public static void loadAppDatabaseFile(File file, boolean skipEmptyIcons, boolean skipDataIcons) {
        try {
            FileReader reader = new FileReader(file);
            StringBuilder out = new StringBuilder();
            char[] buf = new char[4096];
            int read;
            while (reader.ready()) {
                read = reader.read(buf);
                for (int i = 0; i < read; i++)
                    out.append(buf[i]);
            }
            JSONArray appArray = new JSONArray(out.toString());
            apps.clear();
            for (Object appObject : appArray) {
                JSONObject appJSON = (JSONObject) appObject;
                if (skipEmptyIcons && appJSON.getString("art").isEmpty()) continue;
                if (skipDataIcons && appJSON.getString("art").startsWith("data")) continue;
                App app = new App(appJSON.getString("name"), appJSON.getString("bundle"));
                for (Object versionObject : appJSON.getJSONArray("versions")) {
                    JSONObject versionJSON = (JSONObject) versionObject;
                    JSONArray array = versionJSON.getJSONArray("urls");
                    LinkedList<App.VersionLink> versionLinks = new LinkedList<>();
                    for (Object objectStd : array) {
                        JSONObject object = (JSONObject) objectStd;
                        JSONObject binary = null;
                        if (object.has("bin")) {
                            binary = object.getJSONObject("bin");
                        }
                        versionLinks.add(new App.VersionLink(Binary.fromJSON(binary), object.getString("url"), object.getString("bv"), object.getLong("fs")));
                    }
                    app.addAppVersionNoSort(versionJSON.getString("ver"),
                            versionLinks.toArray(new App.VersionLink[]{}),
                            versionJSON.getString("support"));
                }
                app.updateArtwork(appJSON.getString("artver"), appJSON.getString("art"));
                app.updateDeveloper(appJSON.getString("devVer"), appJSON.getString("dev"));
                if (appJSON.getBoolean("nN")) {
                    app.usedMetaName();
                }
                app.sortVersions();
                apps.add(app);
            }
        } catch (FileNotFoundException e) {
            System.err.println("File not found! Not importing anything.");
        } catch (Exception e) {
            System.err.println(e);
        }
    }
    
    public static void saveAppDatabaseFile(File file) {
        JSONArray appArray = new JSONArray();
        for (App app : apps) {
            appArray.put(app.getAppJSON());
        }
        try {
            FileWriter writer = new FileWriter(file, false);
            writer.write(appArray.toString());
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to write to file!");
        }
    }
    
    public static List<App> listAppsThatSupportVersion(String version) {
        return apps.parallelStream().filter(app -> app.showAppForVersion(version)).collect(Collectors.toList());
    }
    
    public static App getAppByBundleID(String bundleID) {
        List<App> theApp = apps.parallelStream().filter(app -> (app.getBundleID().equals(bundleID))).collect(Collectors.toList());
        if (theApp.isEmpty()) return null;
        return theApp.get(0);
    }
    
    public static void addApp(App app) {
        if (getAppByBundleID(app.getBundleID()) == null) {
            apps.add(app);
        }
    }
    
    public static List<App> searchApps(String query, String version) {
        return apps.parallelStream()
                .filter(app -> (app.showAppForVersion(version) && app.getName().toLowerCase().contains(query.toLowerCase())))
                .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
    }
    
    public static List<App> searchApps(String query) {
        return apps.parallelStream().filter(app -> app.getName().toLowerCase().contains(query.toLowerCase()))
                .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
    }
    
    public static boolean appUrlAlreadyExists(String url) {
        return !apps.parallelStream().filter(app -> !app.getAllUrls().parallelStream().filter(string -> string.equals(url))
                        .collect(Collectors.toList()).isEmpty()).collect(Collectors.toList()).isEmpty();
    }
}
