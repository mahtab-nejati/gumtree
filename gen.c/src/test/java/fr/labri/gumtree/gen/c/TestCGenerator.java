package fr.labri.gumtree.gen.c;

import java.io.IOException;

import org.junit.Test;
import static org.junit.Assert.*;

import fr.labri.gumtree.tree.ITree;

public class TestCGenerator {
	
	@Test
	public void testSimpleSyntax() throws IOException {
		String input = "int main() { printf(\"Hello world!\"); return 0; }";
		ITree t = new CTreeGenerator().generateFromString(input).getRoot();
		assertEquals(450000, t.getType());
	}
	
}
