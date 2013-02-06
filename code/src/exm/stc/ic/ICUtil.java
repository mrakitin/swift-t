/*
 * Copyright 2013 University of Chicago and Argonne National Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */
package exm.stc.ic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import exm.stc.common.exceptions.STCRuntimeError;
import exm.stc.common.lang.Arg;
import exm.stc.common.lang.Types;
import exm.stc.common.lang.Var;
import exm.stc.ic.tree.ICContinuations.Continuation;
import exm.stc.ic.tree.ICInstructions.Instruction;
import exm.stc.ic.tree.ICTree.Block;
import exm.stc.ic.tree.ICTree.CleanupAction;

/**
 * Miscellaneous useful utilities that are used in multiple places in the intermediate 
 * code
 *
 */
public class ICUtil {

  public static final String indent = "  ";
  
  /** Print a formal argument list, e.g. "(int a, int b, int c)" */
  public static void prettyPrintFormalArgs(StringBuilder sb,
                                                  List<Var> args) {
    boolean first = true;
    sb.append("(");
    for (Var a: args) {
      if (!first) {
        sb.append(", ");
      }
      sb.append(a.type().typeName() + " " + a.name());
      first = false;
    }
    sb.append(")");
  }

  public static void prettyPrintVarInfo(StringBuilder sb,
          List<Var> usedVariables, List<Var> keepOpenVars) {
    boolean printed = false;
    if (usedVariables.size() > 0 ) {
      sb.append("#passin[");
      prettyPrintVarList(sb, usedVariables);
      sb.append("]");
      printed = true;
    }

    if (keepOpenVars.size() > 0) {
      if (printed)
        sb.append(" ");
      sb.append("#keepopen[");
      prettyPrintVarList(sb, keepOpenVars);
      sb.append("]");
    }
  }

  /**
   * print a comma separated list of var names to sb
   * @param sb
   * @param vars
   */
  public static void prettyPrintVarList(StringBuilder sb, 
                Collection<Var> vars) {
    boolean first = true;
    for (Var v: vars) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(v.name());
    }
  }
  
  /**
   * print a comma separated list of var names to sb
   * @param sb
   * @param args
   */
  public static void prettyPrintArgList(StringBuilder sb, 
                Collection<Arg> args) {
    boolean first = true;
    for (Arg a: args) {
      if (first) {
        first = false;
      } else {
        sb.append(", ");
      }
      sb.append(a);
    }
  }

  /**
   * Replace variables by name in list
   * Remove variables with duplicate names
   * @param replacements
   * @param vars
   */
  public static void replaceVarsInList(Map<Var, Arg> replacements,
      List<Var> vars, boolean removeDupes) {
    replaceVarsInList(replacements, vars, removeDupes, true);
  }
  
  public static void replaceVarsInList(Map<Var, Arg> replacements,
        List<Var> vars, boolean removeDupes, boolean removeMapped) {
    // Remove new duplicates
    ArrayList<Var> alreadySeen = null;
    if (removeDupes) {
      alreadySeen = new ArrayList<Var>(vars.size());
    }
    
    ListIterator<Var> it = vars.listIterator();
    while (it.hasNext()) {
      Var v = it.next();
      if (replacements.containsKey(v)) {
        Arg oa = replacements.get(v);
        if (oa.isVar()) {
          if (removeDupes &&  alreadySeen.contains(oa.getVar())) {
            it.remove();
          } else {
            it.set(oa.getVar());
            if (removeDupes) {
              alreadySeen.add(oa.getVar());
            }
          }
        }
      } else {
        if (removeDupes) {
          if (alreadySeen.contains(v)) {
            it.remove();
          } else {
            alreadySeen.add(v);
          }
        }
      }
    }
  }
  
  public static void removeDuplicates(List<Var> varList) {
    ListIterator<Var> it = varList.listIterator();
    HashSet<Var> alreadySeen = new HashSet<Var>();
    while (it.hasNext()) {
      Var v = it.next();
      if (alreadySeen.contains(v)) {
        it.remove();
      } else {
        alreadySeen.add(v);
      }
    }
  }

  public static void replaceOpargsInList(Map<Var, Arg> renames,
      List<Arg> args) {
    replaceOpargsInList(renames, args, false);
  }
  
  public static void replaceOpargsInList(Map<Var, Arg> renames,
      List<Arg> args, boolean nullsOk) {
    if (renames.isEmpty()) {
      return;
    }
    for (int i = 0; i < args.size(); i++) {
      Arg oa = args.get(i);
      if (oa == null) {
        if (nullsOk) {
          continue;
        } else {
          throw new STCRuntimeError("null arg in list: " + args);
        }
      }
      if (oa.isVar()) {
        Arg val = renames.get(oa.getVar());
        if (val != null) {
          args.set(i, val);
        }
      }
    }
  }
  
  /**
   * If oa is a variable with a name in the renames map, replace
   * @param renames
   * @param oa
   * @param nullsOk set to true if oa may be null, otherwise exception
   *    will be thrown
   * @return null if oa is null. If oa is variable and
   *      is in renames, return the replacements.  If it isn't,
   *      return the argument 
   */
  public static Arg replaceOparg(Map<Var, Arg> renames, Arg oa, boolean nullsOk) {
    assert(nullsOk || oa != null);
    if (oa != null && oa.isVar()) {
      Var var = oa.getVar();
      if (renames.containsKey(var)) {
        Arg res = renames.get(var);
        assert(res != null);
        return res;
      }
    }
    return oa;
  }

  public static LinkedList<Instruction> cloneInstructions(
      List<Instruction> instructions) {
    LinkedList<Instruction> output = new LinkedList<Instruction>();
    for (Instruction i : instructions) {
      output.add(i.clone());
    }
    return output;
  }
  
  public static ArrayList<CleanupAction> cloneCleanups(
      List<CleanupAction> actions) {
    ArrayList<CleanupAction> output = new ArrayList<CleanupAction>();
    for (CleanupAction a : actions) {
      output.add(a.clone());
    }
    return output;
  }

  public static ArrayList<Continuation> cloneContinuations(
      List<Continuation> conts, Block parent) {
    ArrayList<Continuation> newContinuations = 
                        new ArrayList<Continuation>(conts.size());
    for (Continuation old: conts) {
      Continuation newC = old.clone();
      newC.setParent(parent);
      newContinuations.add(newC);
    }
    return newContinuations;
  }

  public static ArrayList<Block> cloneBlocks(List<Block> blocks) {
    ArrayList<Block> newBlocks = new ArrayList<Block>(blocks.size());
    for (Block old: blocks) {
      newBlocks.add(old.clone());
    }
    return newBlocks;
  }
  
  /** 
   * Replace the current instruction with the provided sequence
   * After this is done, next() will return the instruction after
   * the inserted sequence
   */
  public static void replaceInsts(ListIterator<Instruction> it, 
              List<Instruction> replacements) {

    if (replacements.size() == 1) {
      it.set(replacements.get(0));
    } else if (replacements.size() == 0) {
      it.remove();
    } else {
      it.set(replacements.get(0));
      List<Instruction> rest = replacements.subList(1, replacements.size());
      for (Instruction newInst: rest) {
        it.add(newInst);
      }
    }
  }
  public static void rewindIterator(
      @SuppressWarnings("rawtypes") ListIterator it, int n) {
    for (int i = 0; i < n; i++) {
      it.previous();
    }
  }  

  /**
   * Return a list of all the variables contained in the
   * input list.  Ignore any non-variable args
   * @param args
   * @return
   */
  public static List<Var> extractVars(List<Arg> args) {
    ArrayList<Var> res = new ArrayList<Var>();
    for (Arg a: args) {
      if (a.isVar()) {
        res.add(a.getVar());
      }
    }
    return res;
  }

  /**
   * Return only variables that need to be blocked on to read results
   */
  public static List<Var> filterBlockingOnly(List<Var> vars) {
    List<Var> res = new ArrayList<Var>();
    for (Var var: vars) {
      if (Types.isScalarFuture(var.type()) ||
          Types.isRef(var.type()) || 
          Types.isArray(var.type())) {
        res.add(var);
      }
    }
    return res;
  }

  public static <T> List<T> filterNulls(Collection<T> list) {
    List<T> res = new ArrayList<T>();
    for (T item: list) {
      if (item != null) {
        res.add(item);
      }
    }
    return res;
  }

  /**
   * Remove all occurences of e in l
   * (Note: standard java List.remove() only removes first occurrence)
   * @param l
   * @param e
   */
  public static <T> void remove(List<T> l, T e) {
    ListIterator<T> it = l.listIterator();
    while (it.hasNext()) {
      if (it.next().equals(e)) {
        it.remove();
      }
    }
  }
}
