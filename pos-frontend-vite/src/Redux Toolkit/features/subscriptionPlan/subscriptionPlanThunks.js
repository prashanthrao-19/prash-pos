import { createAsyncThunk } from "@reduxjs/toolkit";
import api from "@/utils/api";

// Helper function to get JWT token
const getAuthToken = () => {
  const token = localStorage.getItem("jwt");
  if (!token) {
    throw new Error("No JWT token found");
  }
  return token;
};

// Helper function to set auth headers
const getAuthHeaders = () => {
  const token = getAuthToken();
  return {
    Authorization: `Bearer ${token}`,
    "Content-Type": "application/json",
  };
};

// Create Subscription Plan (Admin only)
export const createSubscriptionPlan = createAsyncThunk(
  "subscriptionPlan/create",
  async (plan, { rejectWithValue }) => {
    try {
      const headers = getAuthHeaders();
      const res = await api.post("/api/super-admin/subscription-plans", plan, {
        headers,
      });
      console.log("create subscription plan - ", res.data);
      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data?.message || "Failed to create subscription plan"
      );
    }
  }
);

// Update Subscription Plan (Admin only)
export const updateSubscriptionPlan = createAsyncThunk(
  "subscriptionPlan/update",
  async ({ id, plan }, { rejectWithValue }) => {
    try {
      const headers = getAuthHeaders();
      const res = await api.put(
        `/api/super-admin/subscription-plans/${id}`,
        plan,
        { headers }
      );
      console.log("update subscription plan - ", res.data);
      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data?.message || "Failed to update subscription plan"
      );
    }
  }
);

// Get All Subscription Plans (public - no auth required)
export const getAllSubscriptionPlans = createAsyncThunk(
  "subscriptionPlan/getAll",
  async (_, { rejectWithValue }) => {
    try {
      // Uses public /api/subscription-plans endpoint (permitAll in SecurityConfig)
      const res = await api.get("/api/subscription-plans");
      console.log("get all subscription plans - ", res.data);
      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch subscription plans"
      );
    }
  }
);

// Get Subscription Plan by ID (public - no auth required)
export const getSubscriptionPlanById = createAsyncThunk(
  "subscriptionPlan/getById",
  async (id, { rejectWithValue }) => {
    try {
      // Uses public /api/subscription-plans endpoint (permitAll in SecurityConfig)
      const res = await api.get(`/api/subscription-plans/${id}`);
      console.log("get subscription plan by id - ", res.data);
      return res.data;
    } catch (err) {
      return rejectWithValue(
        err.response?.data?.message || "Failed to fetch subscription plan"
      );
    }
  }
);

// Delete Subscription Plan (Admin only)
export const deleteSubscriptionPlan = createAsyncThunk(
  "subscriptionPlan/delete",
  async (id, { rejectWithValue }) => {
    try {
      const headers = getAuthHeaders();
      await api.delete(`/api/super-admin/subscription-plans/${id}`, {
        headers,
      });
      console.log("delete subscription plan - ", id);
      return id;
    } catch (err) {
      return rejectWithValue(
        err.response?.data?.message || "Failed to delete subscription plan"
      );
    }
  }
);
