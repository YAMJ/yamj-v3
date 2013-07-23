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
    <name>Example Skin</name>
    <version>0.0.1</version>
    <date>2013-07-23</date>
    <description>Description line 1</description>
    <description>Description line 2</description>
    <description>Description line 3</description>
    <url>http://Where.to.com/download/this/skin.zip</url>
    <image>ImageOfTheSkin.jpg</image>
</skin>