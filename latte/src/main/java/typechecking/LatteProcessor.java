package typechecking;

import java.util.ArrayList;
import java.util.List;

import context.ClassLevelMaps;
import context.PermissionEnvironment;
import context.SymbolicEnvironment;
import spoon.processing.AbstractProcessor;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.factory.Factory;

public class LatteProcessor extends AbstractProcessor<CtPackage> {

    List<CtPackage> visitedPackages = new ArrayList<>();
    Factory factory;

    public LatteProcessor(Factory factory) {
        this.factory = factory;
    }

    @Override
    public void process(CtPackage pkg) {
        SymbolicEnvironment se = new SymbolicEnvironment();
        PermissionEnvironment pe = new PermissionEnvironment();
        ClassLevelMaps mtc = new ClassLevelMaps();
        
        if (!visitedPackages.contains(pkg)) {
            visitedPackages.add(pkg);
            pkg.accept(new ExternalRefinementFirstPass( se, pe, mtc));
            pkg.accept(new LatteClassFirstPass( se, pe, mtc));
            pkg.accept(new LatteTypeChecker( se, pe, mtc));
        }

    }
    
}