package cz.matej21.intellij.kdyby.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.ParameterList;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeAnalyserVisitor;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;


public class RepositoryTypeProvider implements PhpTypeProvider2 {

	public static final char KEY = '\u2100';
	private static final PhpType entityManager = new PhpType().add("Kdyby\\Doctrine\\EntityManager").add("Doctrine\\ORM\\EntityManager");

	@Override
	public char getKey() {
		return KEY;
	}

	@Nullable
	@Override
	public String getType(PsiElement psiElement) {
		if (!(psiElement instanceof MethodReference)) {
			return null;
		}
		MethodReference ref = (MethodReference) psiElement;
		if (ref.getName() == null || (!ref.getName().equals("getDao") && !ref.getName().equals("getRepository"))) {
			return null;
		}
		if (psiElement.getChildren().length == 0) {
			return null;
		}
		PhpType type = ((PhpTypedElement) psiElement.getChildren()[0]).getType();
		if (!type.isConvertibleFrom(entityManager, PhpIndex.getInstance(psiElement.getProject()))) {
			return null;
		}
		ParameterList list = ref.getParameterList();
		if (list == null || list.getChildren().length == 0) {
			return null;
		}
		String className = ElementValueResolver.resolve(list.getFirstChild());
		if (className == null) {
			return null;
		}

		PhpTypeAnalyserVisitor analyzer = new PhpTypeAnalyserVisitor(0);
		psiElement.accept(analyzer);
		return className + "." + analyzer.getType().toString();
	}

	@Override
	public Collection<? extends PhpNamedElement> getBySignature(String s, Project project) {
		String signature = s.substring(s.indexOf(".") + 1);
		if (signature.contains("|")) {
			signature = signature.substring(0, signature.indexOf("|"));
		}

		return PhpIndex.getInstance(project).getBySignature(signature);
	}


}
