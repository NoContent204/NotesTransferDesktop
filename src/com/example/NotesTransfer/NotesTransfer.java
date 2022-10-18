package com.example.NotesTransfer;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;

public class NotesTransfer {

    private static StringBuilder datastr = new StringBuilder();
    private static String authtoken;

    public static void main(String[] args) {
        Authentication();
        ArrayList<String> filenames;
        String filename;
        if (args.length < 1) {
            System.out.println("Not enough arguments. For help use argument -h");
        } else {
            switch (args[0]) {
                case "-d":
                    filenames = getListofFiles();
                    if (args.length==1) { // dowload all files
                        for (String filenametmp : filenames) {
                            System.out.println("Downloading "+filenametmp+"...");
                            String downloadtoken = getDownloadToken(filenametmp);
                            DownloadFiles(downloadtoken, filenametmp);
                        }
                    }else{ //download certain files
                        for (int i=1; i< args.length;i++){
                            filename=args[i];
                            if (filenames.contains(filename)){ // check file exists on server
                                System.out.println("Downloading " + filename + "...");
                                String downloadtoken = getDownloadToken(filename);
                                DownloadFiles(downloadtoken, filename);
                            } else {
                                System.out.println(filename+" does not exist on server");
                            }
                        }
                    }
                    break;
                case "-u":
                    if (args.length > 1) {
                        for (int i = 1; i < args.length; i++) {
                            filename = args[i];
                            System.out.println("Uploading " + filename + " ...");
                            UploadFiles(filename);
                        }
                    }
                    break;
                case "-h":
                    System.out.println("This program is used to download and upload text files for easy transfer between here and my phone");
                    System.out.println("If you're not me then I don't know how you got a hold of this. If you don't have my permission fuck off");
                    System.out.println("Usage [ARGUMENTS] [FILES]:");
                    System.out.println("-h: print this help screen and exit");
                    System.out.println("-u: upload the text files listed");
                    System.out.println("-d: either download all files on server and overwrite files of same name in current directory or specify which files you want to download");
                    break;
                default:
                    System.out.println("Invalid arguments. Use -h for help");
                    break;
            }
        }
    }

    public static void Authentication(){
        Thread Auth = new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                datastr.setLength(0);
                String data="";
                //String finalFilename = filename;
                URL url = new URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=APITOKEN");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");

                InputStream stream;
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                String info = "{\"email\":\"EMAILFORFIREBASEUSER\",\"password\":\"PASSWORDFORFIREBASEUSER\",\"returnSecureToken\":true}";
                osw.write(info);
                osw.flush();
                osw.close();
                os.close();
                urlConnection.connect();
                if (urlConnection.getResponseCode()==200){
                    stream = urlConnection.getInputStream();
                    BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
                    //temp string to hold each line
                    String inputLine="";

                    while(inputLine !=null){
                        inputLine=bin.readLine();
                        if (inputLine!=null) {
                            if(inputLine.contains("idToken")){
                                authtoken = inputLine;
                                authtoken = authtoken.replace("\"idToken\": ","");
                                authtoken = authtoken.replace("\"","");
                                authtoken = authtoken.replace(",","");
                                authtoken = authtoken.trim();
                            }
                            datastr.append(inputLine+"\n");
                        }
                        //data = data + inputLine;
                    }
                    data = datastr.toString();

                }else{
                    System.out.println(urlConnection.getResponseMessage());
                    //stream = urlConnection.getErrorStream();
                }

                //System.out.println(urlConnection.getErrorStream().toString());
                //System.out.println(urlConnection.getURL().toString());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

        });
        Auth.start();
        try{
            Auth.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }


    public static void DownloadFiles(String downloadtoken, String filename){
        Thread Download = new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                datastr.setLength(0);
                String data="";
                //String finalFilename = filename;
                URL url = new URL("https://firebasestorage.googleapis.com/v0/b/NAMEOFFIREBASEPROJECT.appspot.com/o/"+filename+"?alt=media&token="+downloadtoken);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+authtoken);
                //urlConnection.setRequestProperty("Accept", "text/plain");
                //urlConnection.setRequestProperty("Content-Type", "text/plain");
                InputStream stream;
                if (urlConnection.getResponseCode()==200){
                    stream = urlConnection.getInputStream();

                }else{
                    System.out.println(urlConnection.getResponseMessage());
                    stream = urlConnection.getErrorStream();
                }
                //System.out.println(urlConnection.getErrorStream().toString());
                //System.out.println(urlConnection.getURL().toString());
                BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
                //temp string to hold each line
                String inputLine="";

                while(inputLine !=null){
                    inputLine=bin.readLine();
                    if (inputLine!=null) {
                        datastr.append(inputLine+"\n");
                    }
                    //data = data + inputLine;
                }
                data = datastr.toString();
                //System.out.println(data);
                try  (BufferedWriter writer = Files.newBufferedWriter(Path.of(filename))) {
                    writer.write(data);
                }catch (IOException e){
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

        });
        Download.start();
        try{
            Download.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }

    }

    public static String getDownloadToken(String filename){
        StringBuilder token = new StringBuilder();
        Thread Download = new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String data="";
                //String finalFilename = filename;
                URL url = new URL("https://firebasestorage.googleapis.com/v0/b/NAMEOFFIREBASEPROJECT.appspot.com/o/"+filename);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+authtoken);
                //urlConnection.setRequestProperty("Accept", "application/json");
                //urlConnection.setRequestProperty("Content-Type", "text/plain");
                InputStream stream;
                if (urlConnection.getResponseCode()==200){
                    stream = urlConnection.getInputStream();

                }else{
                    stream = urlConnection.getErrorStream();
                }
                //System.out.println(urlConnection.getErrorStream().toString());
                //System.out.println(urlConnection.getURL().toString());
                BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
                //temp string to hold each line
                String inputLine="";

                while(inputLine !=null){
                    inputLine=bin.readLine();
                    if (inputLine!=null) {
                        if (inputLine.contains("downloadTokens")) {
                            data = inputLine;
                        }
                    }
                    //datastr.append(inputLine);
                    //data = data + inputLine;
                }
                //data = datastr.toString();
                //"downloadTokens": "81ec3675-ec9f-4c87-b539-cc1b308de492"
                data = data.replace("\"downloadTokens\": ","");
                data = data.replace("\"","");
                data = data.trim();
                //System.out.println(data);
                token.append(data);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }    //filenames = getListofFiles();
        //UploadFiles("test.txt");
        //DownloadFiles();

        });
        Download.start();
        try{
            Download.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return token.toString();


    }

    public static ArrayList<String> getListofFiles(){
        ArrayList<String> filenames = new ArrayList<String>();
        Thread Download = new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String data="";
                //String finalFilename = filename;
                URL url = new URL("https://firebasestorage.googleapis.com/v0/b/NAMEOFFIREBASEPROJECT.appspot.com/o/");
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+authtoken);
                //urlConnection.setRequestProperty("Accept", "application/json");
                //urlConnection.setRequestProperty("Content-Type", "text/plain");
                InputStream stream;
                if (urlConnection.getResponseCode()==200){
                    stream = urlConnection.getInputStream();

                }else{
                    stream = urlConnection.getErrorStream();
                }
                //System.out.println(urlConnection.getErrorStream().toString());
                //System.out.println(urlConnection.getURL().toString());
                BufferedReader bin = new BufferedReader(new InputStreamReader(stream));
                //temp string to hold each line
                String inputLine="";

                while(inputLine !=null){
                    inputLine=bin.readLine();
                    if (inputLine!=null) {
                        if (inputLine.contains("name")) {
                            data = inputLine;
                            data = data.replace("\"name\": ","");
                            data = data.replace("\"","");
                            data = data.replace(",","");
                            data = data.trim();
                            filenames.add(data);
                        }
                    }
                    //datastr.append(inputLine);
                    //data = data + inputLine;
                }
                //System.out.println(filenames.get(0));
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

        });
        Download.start();
        try {
            Download.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
        return filenames;

    }

    public static void UploadFiles(String filename){
        String text="";
        Path readFilePath = Paths.get(filename);
        try  (Scanner reader = new Scanner(readFilePath))
        {
            while (reader.hasNext()){
                text =text+reader.nextLine()+"\n";
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        String finalText = text;
        Thread Upload = new Thread(() -> {
            HttpURLConnection urlConnection = null;
            try {
                String data="";
                String finalFilename = filename;
                URL url = new URL("https://firebasestorage.googleapis.com/v0/b/NAMEOFFIREBASEPROJECT.appspot.com/o/" + finalFilename + "?&token="+authtoken);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("POST");
                urlConnection.setRequestProperty ("Authorization", "Bearer "+authtoken);
                //urlConnection.setRequestProperty("Accept", "application/json");
                urlConnection.setRequestProperty("Content-Type", "text/plain");
                urlConnection.setDoOutput(true);
                OutputStream os = urlConnection.getOutputStream();
                OutputStreamWriter osw = new OutputStreamWriter(os, "UTF-8");
                osw.write(finalText);
                osw.flush();
                osw.close();
                os.close();  //don't forget to close the OutputStream
                urlConnection.connect();
                InputStream stream = urlConnection.getInputStream();
                System.out.println(urlConnection.getResponseMessage());
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }

        });
        Upload.start();
        try{
            Upload.join();
        }catch (InterruptedException e){
            e.printStackTrace();
        }
    }
}
