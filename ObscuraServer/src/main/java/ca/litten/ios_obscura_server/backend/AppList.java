package ca.litten.ios_obscura_server.backend;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class AppList {
    private static final ArrayList<App> apps = new ArrayList<>();
    
    public static void loadAppDatabaseFile(File file) {
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
            for (Object appObject : appArray) {
                JSONObject appJSON = (JSONObject) appObject;
                App app = new App(appJSON.getString("name"), appJSON.getString("bundle"));
                for (Object versionObject : appJSON.getJSONArray("versions")) {
                    JSONObject versionJSON = (JSONObject) versionObject;
                    app.addAppVersionNoSort(versionJSON.getString("ver"),
                            versionJSON.getJSONArray("urls").toList().toArray(new String[]{}),
                            versionJSON.getString("support"));
                }
                app.updateArtwork(appJSON.getString("artver"), appJSON.getString("art"));
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
            System.out.println(appArray.toString());
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
        while (true) {
            try {
                return apps.parallelStream()
                        .filter(app -> (app.showAppForVersion(version) && app.getName().contains(query)))
                        .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
            } catch (ConcurrentModificationException e) {
                // Do nothing
            }
        }
    }
    
    public static List<App> searchApps(String query) {
        while (true) {
            try {
                return apps.parallelStream().filter(app -> app.getName().contains(query))
                        .sorted(Comparator.comparingInt(o -> o.getName().length())).collect(Collectors.toList());
            } catch (ConcurrentModificationException e) {
                // Do nothing
            }
        }
    }
    
    public static boolean appUrlAlreadyExists(String url) {
        while (true) {
            try {
                return !apps.parallelStream().filter(app -> !app.getSupportedAppVersions("99999999").values().parallelStream()
                                .filter(strings -> !Arrays.stream(strings).filter(string -> string.equals(url))
                                        .collect(Collectors.toList()).isEmpty()).collect(Collectors.toList()).isEmpty())
                        .collect(Collectors.toList()).isEmpty();
            } catch (ConcurrentModificationException e) {
                // Do nothing
            }
        }
    }
}
