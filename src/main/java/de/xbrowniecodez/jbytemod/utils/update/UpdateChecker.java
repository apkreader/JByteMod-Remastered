package de.xbrowniecodez.jbytemod.utils.update;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

import javax.swing.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.xbrowniecodez.jbytemod.Main;
import de.xbrowniecodez.jbytemod.utils.update.objects.Version;
import de.xbrowniecodez.jbytemod.utils.update.ui.UpdateDialogFrame;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class UpdateChecker {
	private Version latestVersion;

	public UpdateChecker() {
		check();
	}

	public void check() {
		 Main.INSTANCE.getLogger().log("Checking for updates...");
		JsonObject releaseInfo = fetchLatestReleaseInfo();
		if (releaseInfo != null) {
			latestVersion = new Version(releaseInfo.get("name").getAsString());
			String changelog = releaseInfo.get("body").getAsString();
			if(latestVersion.isNewer(Main.INSTANCE.getJByteMod().getVersion()))
				showUpdateDialog(String.valueOf(latestVersion), changelog);
		}
	}

	@SneakyThrows
	private JsonObject fetchLatestReleaseInfo() {
		URL url = new URL("https://api.github.com/repos/xBrownieCodezV2/JByteMod-Remastered/releases/latest");
		URLConnection connection = url.openConnection();

		try (InputStream inputStream = connection.getInputStream();
			 InputStreamReader reader = new InputStreamReader(inputStream)) {
			return JsonParser.parseReader(reader).getAsJsonObject();
		}
	}

	private void showUpdateDialog(String latestVersion, String changelog) {
		SwingUtilities.invokeLater(() -> {
			JFrame updateDialogFrame = new UpdateDialogFrame(latestVersion, changelog); 
			updateDialogFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
			updateDialogFrame.setSize(600, 500);
			updateDialogFrame.setLocationRelativeTo(null);
			updateDialogFrame.setVisible(true);
		});
	}

}
