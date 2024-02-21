/*
 * Copyright (C) 2022 - 2023 Elytrium
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.elytrium.fastmotd.dummy;

import com.velocitypowered.api.network.ProtocolVersion;
import com.velocitypowered.api.permission.Tristate;
import com.velocitypowered.api.proxy.ConnectionRequestBuilder;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.crypto.IdentifiedKey;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.player.PlayerSettings;
import com.velocitypowered.api.proxy.player.ResourcePackInfo;
import com.velocitypowered.api.proxy.player.TabList;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.util.GameProfile;
import com.velocitypowered.api.util.ModInfo;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.identity.Identity;
import net.kyori.adventure.text.Component;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jetbrains.annotations.NotNull;

public class DummyPlayer implements Player {

  public static final DummyPlayer INSTANCE = new DummyPlayer();

  @Override
  public String getUsername() {
    return null;
  }

  @Override
  public @Nullable Locale getEffectiveLocale() {
    return null;
  }

  @Override
  public void setEffectiveLocale(Locale locale) {

  }

  @Override
  public UUID getUniqueId() {
    return null;
  }

  @Override
  public Optional<ServerConnection> getCurrentServer() {
    return Optional.empty();
  }

  @Override
  public PlayerSettings getPlayerSettings() {
    return null;
  }

  @Override
  public boolean hasSentPlayerSettings() {
    return false;
  }

  @Override
  public Optional<ModInfo> getModInfo() {
    return Optional.empty();
  }

  @Override
  public long getPing() {
    return 0;
  }

  @Override
  public boolean isOnlineMode() {
    return false;
  }

  @Override
  public ConnectionRequestBuilder createConnectionRequest(RegisteredServer server) {
    return null;
  }

  @Override
  public List<GameProfile.Property> getGameProfileProperties() {
    return null;
  }

  @Override
  public void setGameProfileProperties(List<GameProfile.Property> properties) {

  }

  @Override
  public GameProfile getGameProfile() {
    return null;
  }

  @Override
  public void clearPlayerListHeaderAndFooter() {

  }

  @Override
  public Component getPlayerListHeader() {
    return null;
  }

  @Override
  public Component getPlayerListFooter() {
    return null;
  }

  @Override
  public TabList getTabList() {
    return null;
  }

  @Override
  public void disconnect(Component reason) {

  }

  @Override
  public void spoofChatInput(String input) {

  }

  @Override
  public void sendResourcePack(String url) {

  }

  @Override
  public void sendResourcePack(String url, byte[] hash) {

  }

  @Override
  public void sendResourcePackOffer(ResourcePackInfo packInfo) {

  }

  @Override
  public @Nullable ResourcePackInfo getAppliedResourcePack() {
    return null;
  }

  @Override
  public @Nullable ResourcePackInfo getPendingResourcePack() {
    return null;
  }

  @Override
  public Collection<ResourcePackInfo> getAppliedResourcePacks() {
    return null;
  }

  @Override
  public Collection<ResourcePackInfo> getPendingResourcePacks() {
    return null;
  }

  @Override
  public boolean sendPluginMessage(ChannelIdentifier identifier, byte[] data) {
    return false;
  }

  @Override
  public @Nullable String getClientBrand() {
    return null;
  }

  @Override
  public void addCustomChatCompletions(@NotNull Collection<String> collection) {

  }

  @Override
  public void removeCustomChatCompletions(@NotNull Collection<String> collection) {

  }

  @Override
  public void setCustomChatCompletions(@NotNull Collection<String> collection) {

  }

  @Override
  public Tristate getPermissionValue(String permission) {
    return null;
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return null;
  }

  @Override
  public Optional<InetSocketAddress> getVirtualHost() {
    return Optional.empty();
  }

  @Override
  public boolean isActive() {
    return false;
  }

  @Override
  public ProtocolVersion getProtocolVersion() {
    return null;
  }

  @Override
  public IdentifiedKey getIdentifiedKey() {
    return null;
  }

  @Override
  public @NotNull Identity identity() {
    throw new IllegalStateException();
  }
}
