package final_project;

public class useCPU {
	//protected static ExecutorService threadPool = Executors.newFixedThreadPool(10);
	
	public static void main(String args[]){
		for ( int x = Integer.MAX_VALUE; x > 0; x--){
			for( int y = 3; y < (int)Math.sqrt(x); y ++){
				if (x % y == 0 );
			}
			System.out.println(x);
		}
		return;
	}
}