/*
 * Copyright (C) 2022 Elytrium
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

package net.elytrium.fastmotd.utils;

import com.velocitypowered.api.network.ProtocolVersion;
import io.netty.buffer.ByteBuf;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import net.elytrium.fastmotd.FastMOTD;
import net.elytrium.fastmotd.Settings;
import net.elytrium.fastmotd.holder.MOTDHolder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.ComponentSerializer;

public class MOTDGenerator {

  private final FastMOTD plugin;
  private final ComponentSerializer<Component, Component, String> serializer;
  private final int holdersAmount = Settings.IMP.MAIN.DESCRIPTIONS.size() * this.notZero(Settings.IMP.MAIN.FAVICONS.size());
  private final MOTDHolder[] holders = new MOTDHolder[this.holdersAmount];

  public MOTDGenerator(FastMOTD plugin, ComponentSerializer<Component, Component, String> serializer) {
    this.plugin = plugin;
    this.serializer = serializer;
  }

  public void generate() {
    List<String> favicons = Settings.IMP.MAIN.FAVICONS;
    int faviconsSize = favicons.size();

    if (faviconsSize == 0) {
      this.generate(0, null);
    }

    for (int i = 0; i < faviconsSize; i++) {
      String faviconLocation = favicons.get(i);
      try {
        String base64Favicon = this.getFavicon(Paths.get(faviconLocation));
        this.generate(i, base64Favicon);
      } catch (IOException e) {
        this.plugin.getLogger().warn("Failed to load favicon {}. Ensure that the file exists or modify config.yml", faviconLocation);
        this.generate(i, null);
      }
    }
  }

  private void generate(int i, String favicon) {
    List<String> descriptions = Settings.IMP.MAIN.DESCRIPTIONS;
    for (int j = 0, descriptionsSize = descriptions.size(); j < descriptionsSize; j++) {
      String description = descriptions.get(j);
      this.holders[i * descriptionsSize + j] = new MOTDHolder(this.serializer, description, favicon);
    }
  }

  private String getFavicon(Path faviconLocation) throws IOException {
    BufferedImage image;
    try (ImageInputStream in = ImageIO.createImageInputStream(Files.newInputStream(faviconLocation))) {
      ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
      reader.setInput(in, true, false);
      image = reader.read(0);
      reader.dispose();
    }

    ByteArrayOutputStream outBytes = new ByteArrayOutputStream();
    try (ImageOutputStream out = ImageIO.createImageOutputStream(outBytes)) {
      ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
      ImageWriter writer = ImageIO.getImageWriters(type, "png").next();

      ImageWriteParam param = writer.getDefaultWriteParam();
      if (param.canWriteCompressed()) {
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality((float) Settings.IMP.MAIN.PNG_QUALITY);
      }

      writer.setOutput(out);
      writer.write(null, new IIOImage(image, null, null), param);
      writer.dispose();
    }

    String favicon = "data:image/png;base64," + Base64.getEncoder().encodeToString(outBytes.toByteArray());
    outBytes.close();

    return favicon;
  }

  public void update() {
    int online = this.plugin.getServer().getPlayerCount() + Settings.IMP.MAIN.FAKE_ONLINE_ADD_SINGLE;
    online = online * (Settings.IMP.MAIN.FAKE_ONLINE_ADD_PERCENT + 100) / 100;

    int max;
    MaxCountType type = MaxCountType.valueOf(Settings.IMP.MAIN.MAX_COUNT_TYPE);
    switch (type) {
      case ADD_SOME:
        max = online + Settings.IMP.MAIN.MAX_COUNT;
        break;
      case VARIABLE:
        max = Settings.IMP.MAIN.MAX_COUNT;
        break;
      default:
        throw new IllegalStateException("Unexpected value: " + type);
    }

    for (MOTDHolder holder : this.holders) {
      holder.replaceOnline(max, online);
    }
  }

  public ByteBuf getNext(ProtocolVersion version) {
    return this.holders[ThreadLocalRandom.current().nextInt(this.holdersAmount)].getByteBuf(version);
  }

  public void dispose() {
    for (MOTDHolder holder : this.holders) {
      holder.dispose();
    }
  }

  private int notZero(int input) {
    if (input == 0) {
      return 1;
    } else {
      return input;
    }
  }

  private enum MaxCountType {

    VARIABLE,
    ADD_SOME
  }
}
