import "@testing-library/jest-dom";
import { render, screen } from "@testing-library/react";

jest.mock("next/navigation", () => ({
  useRouter: () => ({ push: jest.fn(), replace: jest.fn() })
}));

jest.mock("@/lib/auth", () => ({
  login: jest.fn(),
  signup: jest.fn()
}));

import AuthForm from "@/components/AuthForm";

describe("AuthForm — login mode", () => {
  it("renders login form fields", () => {
    render(<AuthForm mode="login" />);
    expect(screen.getByPlaceholderText("you@example.com")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("••••••••")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "로그인" })).toBeInTheDocument();
  });

  it("has a link to signup page", () => {
    render(<AuthForm mode="login" />);
    expect(screen.getByRole("link", { name: "회원가입" })).toHaveAttribute("href", "/signup");
  });
});

describe("AuthForm — signup mode", () => {
  it("renders signup form fields including name", () => {
    render(<AuthForm mode="signup" />);
    expect(screen.getByPlaceholderText("홍길동")).toBeInTheDocument();
    expect(screen.getByPlaceholderText("you@example.com")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "회원가입" })).toBeInTheDocument();
  });

  it("has a link to login page", () => {
    render(<AuthForm mode="signup" />);
    expect(screen.getByRole("link", { name: "로그인" })).toHaveAttribute("href", "/login");
  });
});
