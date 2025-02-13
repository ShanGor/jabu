package cn.gzten.jabu.util;

import cn.gzten.jabu.annotation.PathVar;
import cn.gzten.jabu.annotation.Qualifier;
import cn.gzten.jabu.annotation.QueryParam;
import cn.gzten.jabu.annotation.RequestBody;
import cn.gzten.jabu.annotation.processor.BeanProcessor;
import cn.gzten.jabu.exception.JabuRequestError;
import cn.gzten.jabu.core.JabuContext;
import com.google.gson.reflect.TypeToken;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.TypeName;
import org.eclipse.jetty.util.StringUtil;

import javax.lang.model.element.Element;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class JabuProcessorUtil {
    public static void doTypeMapping(List<CodeBlock> codes, VariableElement p, String paramName) {
        var paramType = ClassName.get(p.asType());
        if (paramType.equals(TypeName.get(String.class))) {
            var template = "String _$N = _$N_str;\n";
            codes.add(CodeBlock.of(template, paramName, paramName));
        } else if (paramType.equals(TypeName.INT) || paramType.equals(TypeName.INT.box())) {
            var template = "Integer _$N = $T.isBlank(_$N_str)?null:Integer.parseInt(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.LONG) || paramType.equals(TypeName.LONG.box())) {
            var template = "Long _$N = $T.isBlank(_$N_str)?null:Long.parseLong(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.BOOLEAN) || paramType.equals(TypeName.BOOLEAN.box())) {
            var template = "Boolean _$N = $T.isBlank(_$N_str)?null:Boolean.parseBoolean(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.BYTE) || paramType.equals(TypeName.BYTE.box())) {
            var template = "Byte _$N = $T.isBlank(_$N_str)?null:Byte.parseByte(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.SHORT) || paramType.equals(TypeName.SHORT.box())) {
            var template = "Short _$N = $T.isBlank(_$N_str)?null:Short.parseShort(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.CHAR) || paramType.equals(TypeName.CHAR.box())) {
            var template = "Char _$N = $T.isBlank(_$N_str)?null:_$N_str.charAt(0);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else if (paramType.equals(TypeName.FLOAT) || paramType.equals(TypeName.FLOAT.box())) {
            var template = "Float _$N = $T.isBlank(_$N_str)?null:Float.parseFloat(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        }  else if (paramType.equals(TypeName.DOUBLE) || paramType.equals(TypeName.DOUBLE.box())) {
            var template = "Double _$N = $T.isBlank(_$N_str)?null:Double.parseDouble(_$N_str);\n";
            codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
        } else {
            var errMsg = "Error at compiling: does not support type: " + ClassName.get(p.asType()).toString();
            System.err.println(errMsg);
            throw new RuntimeException(errMsg);
        }

    }

    public static void doRequestBody(List<CodeBlock> codes, VariableElement p, String paramName) {
        var typeName = TypeName.get(p.asType());
        if (typeName.isPrimitive()) {
            typeName = typeName.box();
        }

        codes.add(CodeBlock.of("$T _$N = null;\n", typeName, paramName));
        codes.add(CodeBlock.of("try(var ins = ctx.getRequestBodyAsStream()) {\n"));
        if (typeName.equals(TypeName.get(String.class)) || typeName.isBoxedPrimitive()) {
            codes.add(CodeBlock.of("  String _$N_str = new String(ins.readAllBytes());\n"));
            if (typeName.equals(TypeName.get(String.class))) {
                var template = "  _$N = _$N_str;\n";
                codes.add(CodeBlock.of(template, paramName, paramName));
            } else if (typeName.equals(TypeName.INT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Integer.parseInt(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.LONG.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Long.parseLong(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            }else if (typeName.equals(TypeName.BOOLEAN.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Boolean.parseBoolean(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.BYTE.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Byte.parseByte(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.SHORT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Short.parseShort(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.CHAR.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:_$N_str.charAt(0);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else if (typeName.equals(TypeName.FLOAT.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Float.parseFloat(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            }  else if (typeName.equals(TypeName.DOUBLE.box())) {
                var template = "  _$N = $T.isBlank(_$N_str)?null:Double.parseDouble(_$N_str);\n";
                codes.add(CodeBlock.of(template, paramName, StringUtil.class, paramName, paramName));
            } else {
                var errMsg = "SimProcessorUtil.doRequestBody: Error at compiling: does not support type: " + typeName;
                System.err.println(errMsg);
                throw new RuntimeException(errMsg);
            }
        } else {
            var template = "  _$N = $T.toObject(ins, new $T<$T>(){});\n";
            codes.add(CodeBlock.of(template, paramName, JsonUtil.class, TypeToken.class, p.asType()));
        }
        codes.add(CodeBlock.of("} catch (RuntimeException | $T e) {\n", IOException.class));
        codes.add(CodeBlock.of("  e.printStackTrace();\n"));
        codes.add(CodeBlock.of("""
                  ctx.returnWith(400, "Request body parsing error: %s".formatted(e.getMessage()));
                """));
        codes.add(CodeBlock.of("} finally {\n"));
        codes.add(CodeBlock.of("  ctx.rewindRequestByteBuffer();\n"));
        codes.add(CodeBlock.of("}\n"));
    }

    public static MethodParameterResult prepareMethodParameters(List<? extends VariableElement> methodParams) {
        var result = new MethodParameterResult();
        var queryParamDefined = false;
        for (var p : methodParams){
            var qp = p.getAnnotation(QueryParam.class);
            var paramName = p.getSimpleName().toString();
            if (qp != null) {
                if (!queryParamDefined) {
                    result.codes.add(CodeBlock.of("var queryParams = ctx.getQueryParams();\n"));
                    queryParamDefined = true;
                }

                var queryParamKey = paramName;
                if (StringUtil.isNotBlank(qp.value())) {
                    queryParamKey = qp.value();
                }
                result.paramNames.add("_" + paramName);

                if (qp.required()) {
                    result.codes.add(CodeBlock.of("if (!queryParams.containsKey($S)) { throw new $T($S);}\n",
                            queryParamKey, JabuRequestError.class, "Not found parameter: %s".formatted(queryParamKey)));
                }

                // get it as String first.
                result.codes.add(CodeBlock.of("String _$N_str = queryParams.getValue($S);\n", paramName, queryParamKey));

                JabuProcessorUtil.doTypeMapping(result.codes, p, paramName);

                continue;
            }
            var pv = p.getAnnotation(PathVar.class);
            if (pv != null) {
                var queryParamKey = paramName;
                if (StringUtil.isNotBlank(pv.value())) {
                    queryParamKey = pv.value();
                }
                result.paramNames.add("_" + paramName);

                // try to get the path var
                result.codes.add(CodeBlock.of("$T<String> _$N_str_opt = ctx.getPathVar($S);\n", Optional.class, paramName, queryParamKey));

                // PathVar, once defined, it is mandatory
                result.codes.add(CodeBlock.of("if (_$N_str_opt.isEmpty()) { throw new $T($S);}\n",
                        paramName, JabuRequestError.class, "Not found PathVar: %s".formatted(queryParamKey)));

                // get it as String first.
                result.codes.add(CodeBlock.of("String _$N_str = _$N_str_opt.get();\n", paramName, paramName));


                JabuProcessorUtil.doTypeMapping(result.codes, p, paramName);
                continue;
            }

            var rb = p.getAnnotation(RequestBody.class);
            if (rb != null) {
                result.paramNames.add("_" + paramName);
                JabuProcessorUtil.doRequestBody(result.codes, p, paramName);
                continue;
            }

            // If the parameter is asking for JabuContext, give it
            if (elementIsType(p, JabuContext.class)) {
                result.paramNames.add("_" + paramName);
                result.codes.add(CodeBlock.of("var _$N = ctx;\n", paramName));
                continue;
            }

            // try to locate a bean for it.
            var qualifier = p.getAnnotation(Qualifier.class);
            String beanName;
            if (qualifier != null) {

                if (StringUtil.isNotBlank(qualifier.value())) {
                    beanName = qualifier.value();
                } else {
                    beanName = p.getSimpleName().toString();
                }

            } else {
                var opt = BeanProcessor.getDefaultBeanName(TypeName.get(p.asType()));
                if (opt.isEmpty()) {
                    fail("Not found bean to fill: " + p.getSimpleName());
                }
                beanName = opt.get();
            }

            result.paramNames.add("_" + paramName);
            result.codes.add(CodeBlock.of("var _$N = $N;\n", paramName, beanName));
        }

        return result;
    }

    public static boolean elementIsType(Element e, Class clazz) {
        return typeMirrorIsType(e.asType(), clazz);
    }

    public static boolean typeMirrorIsType(TypeMirror tm, Class clazz) {
        return typeNameIsType(TypeName.get(tm), clazz);
    }

    public static boolean typeNameIsType(TypeName tn, Class clazz) {
        return TypeName.get(clazz).equals(tn);
    }

    public static class MethodParameterResult {
        public List<String> paramNames = new LinkedList<>();
        public List<CodeBlock> codes = new LinkedList<>();
    }

    public static final String toCamelCase(final String str) {
        if (str == null) return null;
        if (str.length() == 1) {
            return str.toLowerCase(Locale.ROOT);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, 1).toLowerCase(Locale.ROOT));
        sb.append(str.substring(1));
        return sb.toString();
    }

    public static final String capitalize(final String str) {
        if (str == null) return null;
        if (str.length() == 1) {
            return str.toUpperCase(Locale.ROOT);
        }
        StringBuilder sb = new StringBuilder();
        sb.append(str.substring(0, 1).toUpperCase(Locale.ROOT));
        sb.append(str.substring(1));
        return sb.toString();
    }

    public static void fail(String message) {
        System.err.println(message);
        throw new RuntimeException(message);
    }
}
