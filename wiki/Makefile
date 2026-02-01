PROJECT_ROOT := $(shell git -C $(dir $(abspath $(lastword $(MAKEFILE_LIST)))) rev-parse --show-toplevel)
WORKTREE_ROOT := $(dir $(abspath $(lastword $(MAKEFILE_LIST))))worktrees

.PHONY: worktree-add worktree-remove worktree-list

## 워크트리 생성: make worktree-add FEAT=auth
worktree-add:
	@if [ -z "$(FEAT)" ]; then echo "Usage: make worktree-add FEAT=<feature-name>"; exit 1; fi
	@cd $(PROJECT_ROOT) && git branch feat/$(FEAT) main 2>/dev/null || echo "Branch feat/$(FEAT) already exists"
	@cd $(PROJECT_ROOT) && git worktree add $(WORKTREE_ROOT)/feat-$(FEAT) feat/$(FEAT)
	@mkdir -p $(WORKTREE_ROOT)/feat-$(FEAT)/wiki/logs
	@touch $(WORKTREE_ROOT)/feat-$(FEAT)/wiki/logs/feat-$(FEAT).md
	@echo ""
	@echo "=== Worktree Created ==="
	@echo "  Path:   $(WORKTREE_ROOT)/feat-$(FEAT)"
	@echo "  Branch: feat/$(FEAT)"
	@echo "  Log:    wiki/logs/feat-$(FEAT).md"
	@echo ""

## 워크트리 제거: make worktree-remove FEAT=auth
worktree-remove:
	@if [ -z "$(FEAT)" ]; then echo "Usage: make worktree-remove FEAT=<feature-name>"; exit 1; fi
	@cd $(PROJECT_ROOT) && git worktree remove $(WORKTREE_ROOT)/feat-$(FEAT) --force 2>/dev/null || echo "Worktree not found"
	@echo "Removed worktree: $(WORKTREE_ROOT)/feat-$(FEAT)"

## 워크트리 목록: make worktree-list
worktree-list:
	@cd $(PROJECT_ROOT) && git worktree list
