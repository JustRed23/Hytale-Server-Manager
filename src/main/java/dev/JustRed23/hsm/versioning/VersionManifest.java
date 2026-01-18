package dev.JustRed23.hsm.versioning;

import java.util.List;
import java.util.Map;

public class VersionManifest {
    public Map<String, String> activeVersions;
    public List<VersionInfo> availableVersions;

    public VersionInfo getForPatchline(String patchline) {
        if (activeVersions == null || activeVersions.isEmpty()) return null;
        if (availableVersions == null || availableVersions.isEmpty()) return null;
        String version = activeVersions.get(patchline);
        if (version == null) return null;
        return availableVersions.stream()
                .filter(v -> v.patchline.equals(patchline) && v.version.equals(version))
                .findFirst()
                .orElse(null);
    }
}
