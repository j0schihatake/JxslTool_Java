@echo off
javac -cp ".;picocli-4.7.4.jar" org/j0schi/*.java org/j0schi/commands/*.java org/j0schi/services/*.java
jar cvfe jox.jar org.j0schi.Main org/j0schi/*.class org/j0schi/commands/*.class org/j0schi/services/*.class