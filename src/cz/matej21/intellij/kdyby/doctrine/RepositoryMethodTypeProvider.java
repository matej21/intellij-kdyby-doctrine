package cz.matej21.intellij.kdyby.doctrine;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.jetbrains.php.PhpIndex;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.jetbrains.php.lang.psi.elements.PhpNamedElement;
import com.jetbrains.php.lang.psi.elements.PhpTypedElement;
import com.jetbrains.php.lang.psi.resolve.types.PhpType;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeAnalyserVisitor;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeProvider2;
import com.jetbrains.php.lang.psi.resolve.types.PhpTypeSignatureKey;
import gnu.trove.THashSet;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

public class RepositoryMethodTypeProvider implements PhpTypeProvider2 {
	public static final char KEY = '\u2101';
	private static final HashSet<String> singleEntityMethods = new HashSet<String>(Arrays.asList("find", "findOneBy"));
	private static final HashSet<String> setEntityMethods = new HashSet<String>(Arrays.asList("findAll", "findBy", "fetch"));


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
		if (psiElement.getChildren().length == 0) {
			return null;
		}
		PhpType type = ((PhpTypedElement) psiElement.getChildren()[0]).getType();
		PhpTypeAnalyserVisitor analyzer = new PhpTypeAnalyserVisitor(0);
		psiElement.accept(analyzer);
		for (String strType : type.getTypes()) {
			if (strType.length() < 2 && strType.charAt(0) == '#' || strType.charAt(1) != RepositoryTypeProvider.KEY) {
				continue;
			}
			String entityType = "";
			if (singleEntityMethods.contains(ref.getName()) || setEntityMethods.contains(ref.getName())) {
				entityType = strType.substring(2, strType.indexOf("."));
				if (setEntityMethods.contains(ref.getName())) {
					entityType += "[]";
				}
			}
			String originalType = strType.substring(strType.indexOf(".") + 1);
			if (originalType.contains("|")) {
				originalType = originalType.substring(0, originalType.indexOf("|"));
			}
			if (originalType.charAt(0) == '#') {
				originalType = "." + PhpTypeSignatureKey.METHOD.sign(originalType + "." + ref.getName());
			} else {
				originalType = "";
			}
			return entityType + originalType;
		}

		return null;
	}

	@Override
	public Collection<? extends PhpNamedElement> getBySignature(String s, Project project) {
		Collection<PhpNamedElement> result = new THashSet<PhpNamedElement>();
		PhpIndex phpIndex = PhpIndex.getInstance(project);
		String entityName = s.substring(0, s.indexOf("."));
		if (entityName.length() > 0 && !entityName.endsWith("[]")) {
			result.addAll(phpIndex.getAnyByFQN(entityName));
		}
		String signature = s.substring(s.indexOf(".") + 1);
		if (signature.contains("|")) {
			signature = signature.substring(0, signature.indexOf("|"));
		}
		result.addAll(phpIndex.getBySignature(signature));

		return result;
	}
}
