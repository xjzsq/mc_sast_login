package fun.sast.mc.login.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;

public interface Config {
    File players = new File("config/login-info.json");
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
}
