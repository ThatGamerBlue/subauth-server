package com.thatgamerblue.subauth.server.components.root;

import com.thatgamerblue.subauth.server.util.Env;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class RootHandler {
	@GetMapping("/")
	public String root() {
		return "<html><h2>Connect to:<pre>" + Env.MINECRAFT_SERVER.get() + "</pre> using your Minecraft client to use this service.</h2></html>";
	}
}
