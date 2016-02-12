package com.arellomobile.mvp.compiler;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arellomobile.mvp.DefaultView;
import com.arellomobile.mvp.DefaultViewState;
import com.arellomobile.mvp.InjectViewState;
import com.arellomobile.mvp.MvpPresenter;
import com.arellomobile.mvp.MvpProcessor;

import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;


import static com.arellomobile.mvp.compiler.Util.fillGenerics;

/**
 * Date: 19-Jan-16
 * Time: 19:51
 *
 * @author Alexander Blinov
 */
final class ViewStateProviderClassGenerator extends ClassGenerator<TypeElement>
{
	public static final String MVP_PRESENTER_CLASS = MvpPresenter.class.getCanonicalName();
	public static final String REGEX = ".*<(.*)>.*";

	@Override
	public boolean generate(TypeElement typeElement, ClassGeneratingParams classGeneratingParams)
	{
		String parentClassName = typeElement.toString();

		final String viewClassName = parentClassName.substring(parentClassName.lastIndexOf(".") + 1);

		String view = getViewClassFromGeneric(typeElement);
		view = getViewClassFromAnnotationParams(typeElement, view);
		String viewState = getViewStateClassFromAnnotationParams(typeElement, view);

		String builder = "package " + parentClassName.substring(0, parentClassName.lastIndexOf(".")) + ";\n" +
				"\n" +
				"import com.arellomobile.mvp.ViewStateClassNameProvider;\n" +
				"import java.lang.String;\n" +
				"\n" + "public class " + viewClassName + MvpProcessor.VIEW_STATE_CLASS_NAME_PROVIDER_SUFFIX + " implements ViewStateClassNameProvider" + "\n" +
				"{\n" +
				"\tpublic String getViewStateClassName()\n" +
				"\t{\n";
		if (viewState == null)
		{
			builder += "\t\tthrow new RuntimeException(" + parentClassName + " should has view\");\n";
		}
		else
		{
			builder += "\t\treturn \"" + viewState + "\";\n";
		}
		builder += "\t}\n" +
				"}";

		classGeneratingParams.setName(parentClassName + MvpProcessor.VIEW_STATE_CLASS_NAME_PROVIDER_SUFFIX);
		classGeneratingParams.setBody(builder);

		return true;
	}

	private String getViewClassFromAnnotationParams(TypeElement typeElement, String viewClassName)
	{
		InjectViewState annotation = typeElement.getAnnotation(InjectViewState.class);
		String mvpViewClassName = "";

		if (annotation != null)
		{
			TypeMirror value = null;
			try
			{
				annotation.view();
			}
			catch (MirroredTypeException mte)
			{
				value = mte.getTypeMirror();
			}

			//noinspection ConstantConditions
			final TypeElement mvpViewStateClass = (TypeElement) ((DeclaredType) value).asElement();
			mvpViewClassName = mvpViewStateClass.toString();
		}

		if (!mvpViewClassName.isEmpty() && !DefaultView.class.getName().equals(mvpViewClassName))
		{
			return mvpViewClassName;
		}

		return viewClassName;
	}

	private String getViewStateClassFromAnnotationParams(TypeElement typeElement, String viewClassName)
	{
		InjectViewState annotation = typeElement.getAnnotation(InjectViewState.class);
		String mvpViewStateClassName = "";

		if (annotation != null)
		{
			TypeMirror value = null;
			try
			{
				annotation.value();
			}
			catch (MirroredTypeException mte)
			{
				value = mte.getTypeMirror();
			}

			//noinspection ConstantConditions
			final TypeElement mvpViewStateClass = (TypeElement) ((DeclaredType) value).asElement();
			mvpViewStateClassName = mvpViewStateClass.toString();
		}

		if (!mvpViewStateClassName.isEmpty() && !DefaultViewState.class.getName().equals(mvpViewStateClassName))
		{
			return mvpViewStateClassName;
		}

		if (viewClassName.length() == 0)
		{
			throw new IllegalArgumentException("MvpPresenter(superclass for " + typeElement.getSimpleName() + ") doesn't know, what is target MvpView type of this presenter.");
		}

		return viewClassName + MvpProcessor.VIEW_STATE_SUFFIX;
	}

	private String getViewClassFromGeneric(TypeElement typeElement)
	{
		TypeMirror superclass = typeElement.asType();

		Map<String, String> parentTypes = Collections.emptyMap();

		if (!typeElement.getTypeParameters().isEmpty())
		{
			MvpCompiler.getMessager().printMessage(Diagnostic.Kind.WARNING, "Your " + typeElement.getSimpleName() + " is typed. @InjectViewState may generate wrong code. Your can set view class manually.");
		}

		while (superclass.getKind() != TypeKind.NONE)
		{
			TypeElement superclassElement = (TypeElement) ((DeclaredType) superclass).asElement();

			final List<? extends TypeMirror> typeArguments = ((DeclaredType) superclass).getTypeArguments();
			final List<? extends TypeParameterElement> typeParameters = superclassElement.getTypeParameters();

			if (typeArguments.size() > typeParameters.size())
			{
				throw new IllegalArgumentException("Code generation for interface " + typeElement.getSimpleName() + " failed. Simplify your generics. (" + typeArguments + " vs " + typeParameters + ")");
			}

			Map<String, String> types = new HashMap<>();
			for (int i = 0; i < typeArguments.size(); i++)
			{
				types.put(typeParameters.get(i).toString(), typeArguments.get(i).toString());
			}

			if (superclassElement.toString().equals(MVP_PRESENTER_CLASS))
			{
				return fillGenerics(parentTypes, typeArguments);
			}

			parentTypes = types;

			superclass = superclassElement.getSuperclass();
		}

		return "";
	}
}
