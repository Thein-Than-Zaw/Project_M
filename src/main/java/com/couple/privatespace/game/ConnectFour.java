package com.couple.privatespace.game;

import java.util.*;
import com.couple.privatespace.identity.ApiFailure;

/** GAME-01: server-authoritative gravity, four directions, and a full-board draw. */
public final class ConnectFour {
 private ConnectFour(){}
 public static final String EMPTY=".".repeat(42);
 public record Drop(String board,int cell,List<Integer> line){}
 public static Drop drop(String board,int column,char disc){
  if(column<0||column>6)throw new ApiFailure(400,"INVALID_COLUMN","Choose a column from 1 to 7.");
  int row=5;while(row>=0&&board.charAt(row*7+column)!='.')row--;
  if(row<0)throw new ApiFailure(409,"COLUMN_FULL","This column is full. Choose another column.");
  char[] cells=board.toCharArray();int cell=row*7+column;cells[cell]=disc;String next=new String(cells);
  return new Drop(next,cell,line(next,cell));
 }
 public static List<Integer> line(String board,int cell){
  char disc=board.charAt(cell);if(disc=='.')return List.of();
  for(int[] direction:new int[][]{{0,1},{1,0},{1,1},{1,-1}}){
   var line=new ArrayList<Integer>();line.add(cell);
   for(int sign:new int[]{-1,1})for(int step=1;step<7;step++){
    int row=cell/7+direction[0]*step*sign,column=cell%7+direction[1]*step*sign;
    if(row<0||row>5||column<0||column>6||board.charAt(row*7+column)!=disc)break;line.add(row*7+column);
   }
   if(line.size()>=4)return List.copyOf(line);
  }return List.of();
 }
 public static List<Integer> winningLine(String board){for(int i=0;i<42;i++){var line=line(board,i);if(!line.isEmpty())return line;}return List.of();}
}
