package net.efn.client.account;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

import java.util.UUID;

@Environment(EnvType.CLIENT)
record Credentials(String name, UUID uuid, String accessToken, String secret) {}
