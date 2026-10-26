package com.ipmonitor.office;
import java.net.InetAddress;
import java.util.concurrent.TimeUnit;
class Pinger {
 static boolean valid(String s){String[] p=s.split("\\.",-1);if(p.length!=4)return false;for(String q:p){try{int i=Integer.parseInt(q);if(i<0||i>255||q.length()>3||q.isEmpty())return false;}catch(Exception e){return false;}}return true;}
 static class Result {boolean online;Long latency;Result(boolean o,Long l){online=o;latency=l;}}
 static Result check(String ip){long t=System.nanoTime();Process p=null;try{p=new ProcessBuilder("/system/bin/ping","-c","1","-W","2",ip).redirectErrorStream(true).start();if(p.waitFor(4,TimeUnit.SECONDS)&&p.exitValue()==0)return new Result(true,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-t));}catch(Exception ignored){}finally{if(p!=null)p.destroy();}
 t=System.nanoTime();try{if(InetAddress.getByName(ip).isReachable(2000))return new Result(true,TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-t));}catch(Exception ignored){}return new Result(false,null);}
}
