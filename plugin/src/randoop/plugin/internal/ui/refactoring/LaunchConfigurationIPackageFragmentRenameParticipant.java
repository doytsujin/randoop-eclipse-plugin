package randoop.plugin.internal.ui.refactoring;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;
import org.eclipse.ltk.core.refactoring.participants.CheckConditionsContext;
import org.eclipse.ltk.core.refactoring.participants.RenameParticipant;

import randoop.plugin.internal.core.RandoopCoreUtil;
import randoop.plugin.internal.core.TypeMnemonic;

public class LaunchConfigurationIPackageFragmentRenameParticipant extends RenameParticipant {
  private IPackageFragment fPackageFragment;

  @Override
  protected boolean initialize(Object element) {
    if (element instanceof IPackageFragment) {
      fPackageFragment = (IPackageFragment) element;
      return true;
    }
    return false;
  }

  @Override
  public Change createChange(IProgressMonitor pm) throws CoreException, OperationCanceledException {
    List<Change> changes = new ArrayList<Change>();
    ILaunchConfiguration[] configs = RandoopRefactoringUtil.getRandoopTypeLaunchConfigurations();
    
    List<IType> affectedTypes = new ArrayList<IType>();
    switch (fPackageFragment.getKind()) {
    case IPackageFragmentRoot.K_BINARY:
      for(IClassFile classFile : fPackageFragment.getClassFiles()) {
        IType type = classFile.getType();
        affectedTypes.add(type);
      }
      break;
    case IPackageFragmentRoot.K_SOURCE:
      for(ICompilationUnit compilationUnit : fPackageFragment.getCompilationUnits()) {
        for (IType type : compilationUnit.getAllTypes()) {
          affectedTypes.add(type);
        }
      }
      break;
    }

    HashMap<String, String> newTypeMnemonicByOldTypeMnemonic = new HashMap<String, String>();
    String newPackageName = getArguments().getNewName();

    for (IType type : affectedTypes) {
      TypeMnemonic oldTypeMnemonic = new TypeMnemonic(type);
      
      String oldFullyQualifiedName = oldTypeMnemonic.getFullyQualifiedName();
      String className = RandoopCoreUtil.getClassName(oldFullyQualifiedName);
      String newFullyQualifiedName = RandoopCoreUtil.getFullyQualifiedName(newPackageName, className);

      TypeMnemonic newTypeMnemonic = new TypeMnemonic(oldTypeMnemonic.getJavaProjectName(),
          oldTypeMnemonic.getClasspathKind(), oldTypeMnemonic.getClasspath(), newFullyQualifiedName);

      newTypeMnemonicByOldTypeMnemonic.put(oldTypeMnemonic.toString(), newTypeMnemonic.toString());
    }
    
    for(ILaunchConfiguration config : configs) {
      // TODO: Check if change is needed first
      Change c = new LaunchConfigurationTypeChange(config, newTypeMnemonicByOldTypeMnemonic);
      changes.add(c);
    }
    
    return RandoopRefactoringUtil.createChangeFromList(changes, "Launch configuration updates");
  }
  
  @Override
  public RefactoringStatus checkConditions(IProgressMonitor pm,
      CheckConditionsContext context) throws OperationCanceledException {
    // return OK status
    return new RefactoringStatus();
  }

  @Override
  public String getName() {
    return "Launch configuration participant";
  }
  
}
