package com.couple.privatespace;
import com.couple.privatespace.game.ConnectFour;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ConnectFourTest {
 @Test void gravityAndColumnBoundaries(){var one=ConnectFour.drop(ConnectFour.EMPTY,3,'R');assertEquals(38,one.cell());assertEquals(31,ConnectFour.drop(one.board(),3,'Y').cell());assertThrows(RuntimeException.class,()->ConnectFour.drop(one.board(),-1,'R'));assertThrows(RuntimeException.class,()->ConnectFour.drop(one.board(),7,'R'));}
 @Test void detectsFourDirectionsWithoutWrappingBoardEdges(){
  for(int[] cells:new int[][]{{35,36,37,38},{3,10,17,24},{14,22,30,38},{20,26,32,38}}){char[] b=ConnectFour.EMPTY.toCharArray();for(int cell:cells)b[cell]='R';assertEquals(4,ConnectFour.line(new String(b),cells[0]).size());}
  char[] b=ConnectFour.EMPTY.toCharArray();for(int i:new int[]{5,6,7,8})b[i]='R';assertTrue(ConnectFour.winningLine(new String(b)).isEmpty());
 }
 @Test void completeDrawSequenceHasNoPrematureWinner(){
  int[] sequence={3,4,3,0,4,0,2,4,5,3,5,6,4,2,1,5,3,3,2,4,4,5,3,5,5,0,2,6,2,1,1,2,0,0,6,6,0,1,1,1,6,6};String board=ConnectFour.EMPTY;
  for(int i=0;i<42;i++){var move=ConnectFour.drop(board,sequence[i],i%2==0?'R':'Y');assertTrue(move.line().isEmpty());board=move.board();}assertFalse(board.contains("."));assertTrue(ConnectFour.winningLine(board).isEmpty());
 }
}
