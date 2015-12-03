import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;

public class PageRanker {
	
	static String graphfilename = "web-Google.txt.gz";
	static HashMap<Integer,Integer> degrees = new HashMap<Integer,Integer>();
	static HashMap<Integer,HashSet<Integer>> linkGraph = new HashMap<Integer,HashSet<Integer>>();
	static HashMap<Integer,Double> ranks = new HashMap<Integer,Double>();
	static int N = 875713;
	static int maxiter = 1000;
	static double epsilon = 1e-7;
	static double teleporting = 0.2;
	
	public static void buildLinkGraph(String filename) throws IOException {
		
		GZIPInputStream in = new GZIPInputStream(new FileInputStream(filename));
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		while ((line = reader.readLine()) != null){
			if (line.contains("#")) continue;
			String[] linePart = line.split("\\s+");
			Integer startnode = Integer.parseInt(linePart[0]);
			Integer endnode = Integer.parseInt(linePart[1]);
			if(!linkGraph.containsKey(endnode)) {
				linkGraph.put(endnode, new HashSet<Integer>());
			}
			linkGraph.get(endnode).add(startnode);
			if(!degrees.containsKey(startnode)) degrees.put(startnode, 0);
			degrees.put(startnode,degrees.get(startnode) + 1); // assume no duplicate lines in .txt file
			if(!ranks.containsKey(endnode)) ranks.put(endnode, 0.0);
			if(!ranks.containsKey(startnode)) ranks.put(startnode, 0.0);
		}
		reader.close();
		System.out.println("Build Graph Done.");
		
		double initrank = 1.0/ranks.size();
		for (Integer node : ranks.keySet()){
			ranks.put(node, initrank);
		}
	}
	
	public static boolean iteration(int iter){
		boolean flag = true;
		HashMap<Integer,Double> newranks = new HashMap<Integer,Double>();
		double leak = 1.0;
		
		for (Integer j : ranks.keySet()){
			double rankj = 0.0;
			
			if (linkGraph.containsKey(j)) {
				for (Integer i : linkGraph.get(j)){
					int di = degrees.get(i);
					double ranki = ranks.containsKey(i) ? ranks.get(i) : 0.0;
					rankj += (1-teleporting) * ranki / di ;
					//rankj += teleporting * ranki / ranks.size(); // 这步不对 并不是只有 i可以teleport到j 其它结点也可以
				}
			}
			leak -= rankj;
			newranks.put(j, rankj);
		}

		// normalize and decide flag
		double supplement = leak / newranks.size(); 
		double diff = 0.0;
		for (Integer node : newranks.keySet()){
			newranks.put(node, newranks.get(node) + supplement);
			diff += Math.abs(newranks.get(node) - ranks.get(node));
		}
		System.out.println("iteration "+String.valueOf(iter) +" done.");
		System.out.println(String.valueOf(diff));
		ranks = newranks;
		if (diff > epsilon) flag = false;
		
		return flag;
	}
	
	public static void computeRank(){
		System.out.println("Start Computing ranks...");
		// end condition
		int iter = 0;
		while (iter <= maxiter){
			iter ++;
			boolean flag = iteration(iter);
			if (flag) break;
		}
		System.out.println("Converged.");
	}
	
	public static void main(String[] args) throws IOException{
		buildLinkGraph(graphfilename);
		computeRank();
		double score = 0.0;
		for(Integer node : ranks.keySet()) score += ranks.get(node);
		System.out.println(String.valueOf(score));
		System.out.println(String.valueOf(ranks.get(99)));
		
	}
	
}
