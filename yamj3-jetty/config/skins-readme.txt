YAMJ Skins Folder
=================

This folder is intended as a place to store the skins for YAMJ.

They can be installed manually, or through the YAMJ config pages.

Inside each skin folder, alongside the skin files, there should be two files
provided that can be used to display information to the use through the YAMJ
interface.

"folder.jpg" should be a image of the skin to provide the user with a visual
representation of the skin

"version.xml" is an XML format file containing the details about the skin.
This file should be of the format:

<skin>
  <name>Skin Name</name>
  <version>0.0.0</version>
  <date>01-01-2013</date>
  <message>This is a description of the skin</message>
  <message>It can be as many lines as you need</message>
</skin>
